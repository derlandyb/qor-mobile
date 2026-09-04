import SwiftUI
import shared

/// Errors surfaced by [ProfileViewModel]'s continuation-wrapped bridge over `shared`'s
/// completion-handler-style suspend export — see [EventDetailError] for why this exists.
enum ProfileError: Error {
    case unknown
}

/// I13 — form + save state for [ProfileView] (mirrors Android's `ProfileViewModel`, AUTH-17–19,
/// basic-info scope only). `loadCurrentUser`/`saveProfile`/`applySessionUser` are injectable seams
/// (default: real `SessionStore`/`UpdateProfile` calls via `IosDependencies`) — `shared`'s
/// `SessionStore`/`UpdateProfile` classes can't be subclassed/faked from Swift (no `open`
/// modifier), so the seam lives here instead, at the boundary this class owns.
///
/// **Email edits never apply immediately (AUTH-19).** A successful [saveEmail] restores
/// [emailInput] to the still-current session email and publishes [pendingEmailChange] instead —
/// `ProfileView` observes it and fires the caller's `onEmailChangePending` closure, mirroring
/// Android's `ProfileEvent.EmailChangePending` (routed by A14/I14, not this class).
///
/// Profile-picture editing and birthdate editing are out of scope, matching Android's
/// actually-shipped A13 scope exactly (no image-picker/upload path exists client-side yet; no
/// birthdate field on [domain.user.ProfileUpdateFields]'s contract). LGPD data-rights UI
/// (AUTH-25) is explicitly deferred — see mobile.md's "Not in mobile scope" note.
@MainActor
final class ProfileViewModel: ObservableObject {
    @Published private(set) var user: User?
    @Published var nameInput: String = ""
    @Published var phoneInput: String = ""
    @Published var emailInput: String = ""
    @Published private(set) var isSavingName = false
    @Published private(set) var isSavingPhone = false
    @Published private(set) var isSavingEmail = false
    @Published private(set) var nameError = false
    @Published private(set) var phoneError = false
    @Published private(set) var emailError = false
    @Published var pendingEmailChange: String?

    private let loadCurrentUser: () -> User?
    private let saveProfile: (ProfileUpdateFields) async throws -> User
    private let applySessionUser: (User) -> Void

    init(
        loadCurrentUser: @escaping () -> User? = ProfileViewModel.liveLoadCurrentUser,
        saveProfile: @escaping (ProfileUpdateFields) async throws -> User = ProfileViewModel.liveSaveProfile,
        applySessionUser: @escaping (User) -> Void = ProfileViewModel.liveApplySessionUser
    ) {
        self.loadCurrentUser = loadCurrentUser
        self.saveProfile = saveProfile
        self.applySessionUser = applySessionUser
        refresh()
    }

    /// Reads the currently-loaded fan from the shared session (already populated at
    /// login/restore) — this screen only ever needs to show the session's own user, same
    /// rationale as Android's `ProfileViewModel` reading `SessionStore.currentUser` directly
    /// rather than issuing a fresh `getProfile()` fetch.
    func refresh() {
        guard let user = loadCurrentUser() else { return }
        self.user = user
        nameInput = user.name
        phoneInput = user.phone ?? ""
        emailInput = user.email
    }

    func onNameChange(_ value: String) {
        nameInput = value
        nameError = false
    }

    func onPhoneChange(_ value: String) {
        phoneInput = value
        phoneError = false
    }

    func onEmailChange(_ value: String) {
        emailInput = value
        emailError = false
    }

    /// AUTH-18 — inline-save, reflects immediately (via [applySessionUser]) on success.
    func saveName() async {
        isSavingName = true
        nameError = false
        do {
            let fields = ProfileUpdateFields(name: nameInput, phone: nil, profilePictureUrl: nil, email: nil)
            let updated = try await saveProfile(fields)
            applySessionUser(updated)
            user = updated
            isSavingName = false
        } catch {
            isSavingName = false
            nameError = true
        }
    }

    /// AUTH-18 — inline-save, reflects immediately (via [applySessionUser]) on success.
    func savePhone() async {
        isSavingPhone = true
        phoneError = false
        do {
            let fields = ProfileUpdateFields(name: nil, phone: phoneInput, profilePictureUrl: nil, email: nil)
            let updated = try await saveProfile(fields)
            applySessionUser(updated)
            user = updated
            isSavingPhone = false
        } catch {
            isSavingPhone = false
            phoneError = true
        }
    }

    /// AUTH-19 — unlike [saveName]/[savePhone], a successful call here never applies the change
    /// locally or to the shared session; it only publishes [pendingEmailChange]. See this class's
    /// own KDoc for why.
    func saveEmail() async {
        isSavingEmail = true
        emailError = false
        let newEmail = emailInput
        do {
            let fields = ProfileUpdateFields(name: nil, phone: nil, profilePictureUrl: nil, email: newEmail)
            _ = try await saveProfile(fields)
            isSavingEmail = false
            emailInput = user?.email ?? ""
            pendingEmailChange = newEmail
        } catch {
            isSavingEmail = false
            emailError = true
        }
    }

    static func liveLoadCurrentUser() -> User? {
        // `StateFlow<T>.value` doesn't bridge as a typed Swift property (Kotlin generics erase
        // to `Any?` over Objective-C interop, the same reason `IosDependencies` exists at all).
        IosDependencies.shared.sessionStore().currentUser.value as? User
    }

    static func liveSaveProfile(_ fields: ProfileUpdateFields) async throws -> User {
        try await withCheckedThrowingContinuation { continuation in
            IosDependencies.shared.updateProfile().execute(fields: fields) { user, error in
                if let user {
                    continuation.resume(returning: user)
                } else {
                    continuation.resume(throwing: error ?? ProfileError.unknown)
                }
            }
        }
    }

    static func liveApplySessionUser(_ user: User) {
        IosDependencies.shared.sessionStore().updateCurrentUser(user: user)
    }
}

/// I13 — basic profile display + inline edit (AUTH-17–19), mirroring Android's actually-shipped
/// `ProfileScreen` scope exactly: username/email/phone/birthdate/profile-picture only. **No
/// social widgets, no LGPD data-rights UI** — AUTH-25's full UI is explicitly out of scope,
/// deferred (see mobile.md's "Not in mobile scope" note), even though this task's broader-sounding
/// Requirement line references it.
///
/// This view never pushes navigation itself (I14's job) — [onEmailChangePending] is the seam I14
/// wires, fired when [ProfileViewModel.pendingEmailChange] publishes (AUTH-19: routes to an
/// email-verification screen reusing its existing OTP UX, not duplicated here).
///
/// Birthdate has no edit UI (no field on [ProfileUpdateFields]'s contract to change). Profile-
/// picture editing is stubbed, not wired — no image-picker/upload flow exists anywhere in this
/// app yet, same gap Android's `ProfileScreen` documents; "Alterar foto" renders disabled.
struct ProfileView: View {
    var onEmailChangePending: (String) -> Void

    @StateObject private var viewModel: ProfileViewModel

    @MainActor
    init(
        onEmailChangePending: @escaping (String) -> Void = { _ in },
        viewModel: ProfileViewModel? = nil
    ) {
        self.onEmailChangePending = onEmailChangePending
        _viewModel = StateObject(wrappedValue: viewModel ?? ProfileViewModel())
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: QorSpace.space4) {
                Text(String(localized: "profile_title"))
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextEventTitleLg.shared.SizeSp), weight: .bold))
                    .foregroundStyle(QorColor.textPrimary)

                PlaceholderImage()
                    .frame(height: 120)

                SecondaryButton(text: String(localized: "cta_alterar_foto"), onClick: {}, enabled: false)
                    .accessibilityIdentifier("profile_change_photo")

                Text(String(localized: "profile_picture_change_unavailable"))
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.textTertiary)

                HStack(spacing: QorSpace.space1) {
                    Text("\(String(localized: "field_label_birthdate")):")
                        .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp)))
                        .foregroundStyle(QorColor.textTertiary)
                    Text(viewModel.user?.birthdate ?? "")
                        .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp), weight: .semibold))
                        .foregroundStyle(QorColor.textSecondary)
                        .accessibilityIdentifier("profile_birthdate")
                }

                fieldRow(FieldRowConfig(
                    value: Binding(get: { viewModel.nameInput }, set: viewModel.onNameChange),
                    label: String(localized: "field_label_name"),
                    hasError: viewModel.nameError,
                    isSaving: viewModel.isSavingName,
                    accessibilityId: "profile_name_field"
                ), onSave: { Task { await viewModel.saveName() } })

                fieldRow(FieldRowConfig(
                    value: Binding(get: { viewModel.phoneInput }, set: viewModel.onPhoneChange),
                    label: String(localized: "field_label_phone"),
                    hasError: viewModel.phoneError,
                    isSaving: viewModel.isSavingPhone,
                    accessibilityId: "profile_phone_field"
                ), onSave: { Task { await viewModel.savePhone() } })

                fieldRow(FieldRowConfig(
                    value: Binding(get: { viewModel.emailInput }, set: viewModel.onEmailChange),
                    label: String(localized: "field_label_email"),
                    hasError: viewModel.emailError,
                    isSaving: viewModel.isSavingEmail,
                    accessibilityId: "profile_email_field"
                ), onSave: { Task { await viewModel.saveEmail() } })
            }
            .padding(QorSpace.space4)
        }
        .background(QorColor.bgDeep.ignoresSafeArea())
        .onChange(of: viewModel.pendingEmailChange) { newValue in
            if let newValue {
                onEmailChangePending(newValue)
                viewModel.pendingEmailChange = nil
            }
        }
    }

    private struct FieldRowConfig {
        let value: Binding<String>
        let label: String
        let hasError: Bool
        let isSaving: Bool
        let accessibilityId: String
    }

    @ViewBuilder
    private func fieldRow(_ config: FieldRowConfig, onSave: @escaping () -> Void) -> some View {
        HStack(alignment: .bottom, spacing: QorSpace.space2) {
            QorTextField(
                value: config.value,
                label: config.label,
                errorMessage: config.hasError ? String(localized: "profile_save_error") : nil
            )
            .accessibilityIdentifier(config.accessibilityId)

            PrimaryButton(text: String(localized: "cta_salvar"), onClick: onSave, isLoading: config.isSaving)
                .accessibilityIdentifier("\(config.accessibilityId)_save")
        }
    }
}
