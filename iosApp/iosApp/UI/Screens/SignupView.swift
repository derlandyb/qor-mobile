import SwiftUI
import shared

/**
 * I8 — new-fan signup (auth-fan-profile AUTH-01–AUTH-05, plus a password-confirmation step — see
 * `SignupViewModel`'s doc for why Android's A8 shipped without one). Owns only form + submit UI:
 * on success it calls `onSignupSuccess` with the submitted email — I14 wires this to
 * `EmailVerificationView` with the email pre-filled (mirrors Android's A8 / `qor-website`'s
 * `router.push('/verificar-email?email=...')`). `onNavigateToLogin` is the "já tem conta?" link;
 * this screen does not push any navigation destination itself.
 *
 * **"Cadastrar com Google" is a disabled stub**, same reasoning as Android's A8: no Google
 * Sign-In SDK is wired into `iosApp` yet — the button renders per the design system but is inert.
 */
struct SignupView: View {
    @StateObject private var viewModel: SignupViewModel
    let onNavigateToLogin: () -> Void

    init(
        onSignupSuccess: @escaping (String) -> Void,
        onNavigateToLogin: @escaping () -> Void
    ) {
        _viewModel = StateObject(wrappedValue: SignupViewModel(onSignupSuccess: onSignupSuccess))
        self.onNavigateToLogin = onNavigateToLogin
    }

    /// Test-only seam: lets tests inject a `SignupViewModel` built with a fake
    /// `SignupRegistering`, bypassing `IosDependencies`' real Koin graph.
    init(viewModel: SignupViewModel, onNavigateToLogin: @escaping () -> Void) {
        _viewModel = StateObject(wrappedValue: viewModel)
        self.onNavigateToLogin = onNavigateToLogin
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: QorSpace.space3) {
                Text(String(localized: "signup_title"))
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextEventTitleLg.shared.SizeSp), weight: .bold))
                    .foregroundStyle(QorColor.textPrimary)

                QorTextField(
                    value: Binding(get: { viewModel.uiState.name }, set: viewModel.onNameChange),
                    label: String(localized: "field_label_name"),
                    errorMessage: viewModel.uiState.nameError?.message ?? viewModel.uiState.nameServerError
                )
                .accessibilityIdentifier("signup_name_field")

                EmailField(
                    value: Binding(get: { viewModel.uiState.email }, set: viewModel.onEmailChange),
                    errorMessage: viewModel.uiState.emailError?.message ?? viewModel.uiState.emailServerError
                )
                .accessibilityIdentifier("signup_email_field")

                PasswordField(
                    value: Binding(get: { viewModel.uiState.password }, set: viewModel.onPasswordChange),
                    errorMessage: viewModel.uiState.passwordError?.message ?? viewModel.uiState.passwordServerError
                )
                .accessibilityIdentifier("signup_password_field")

                ConfirmPasswordField(
                    value: Binding(get: { viewModel.uiState.confirmPassword }, set: viewModel.onConfirmPasswordChange),
                    errorMessage: viewModel.uiState.confirmPasswordError?.message
                )
                .accessibilityIdentifier("signup_confirm_password_field")

                QorTextField(
                    value: Binding(get: { viewModel.uiState.birthdate }, set: viewModel.onBirthdateChange),
                    label: String(localized: "field_label_birthdate"),
                    errorMessage: viewModel.uiState.birthdateError?.message ?? viewModel.uiState.birthdateServerError
                )
                .accessibilityIdentifier("signup_birthdate_field")

                ConsentCapture(
                    accepted: Binding(get: { viewModel.uiState.consentAccepted }, set: viewModel.onConsentChange)
                )

                if viewModel.uiState.consentError != nil {
                    Text(String(localized: "signup_error_consent_required"))
                        .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                        .foregroundStyle(QorColor.danger)
                }

                if let submitError = viewModel.uiState.submitError {
                    Text(submitError)
                        .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                        .foregroundStyle(QorColor.danger)
                }

                PrimaryButton(
                    text: String(localized: "cta_cadastrar"),
                    onClick: { Task { await viewModel.submit() } },
                    enabled: viewModel.uiState.consentAccepted,
                    isLoading: viewModel.uiState.isLoading
                )
                .accessibilityIdentifier("signup_submit_button")

                SecondaryButton(
                    text: String(localized: "cta_cadastrar_com_google"),
                    onClick: {},
                    enabled: false
                )
                .accessibilityIdentifier("signup_google_button")

                Text(String(localized: "signup_link_login"))
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.accentBlue)
                    .padding(.top, QorSpace.space1)
                    .onTapGesture(perform: onNavigateToLogin)
                    .accessibilityIdentifier("signup_login_link")
            }
            .padding(QorSpace.space4)
        }
    }
}

/// Signup-local password-confirmation variant of `PasswordField` — kept private to this screen
/// (rather than added to the shared `FormFields.swift`, which I8 was told to reuse, not extend)
/// since it's the only screen with a confirm-password step so far.
private struct ConfirmPasswordField: View {
    @Binding var value: String
    let errorMessage: String?
    @State private var revealed = false

    var body: some View {
        QorTextField(
            value: $value, label: String(localized: "field_label_confirm_password"), errorMessage: errorMessage,
            isSecure: !revealed,
            trailingIcon: {
                let toggleLabel = revealed
                    ? String(localized: "password_toggle_hide")
                    : String(localized: "password_toggle_show")
                return AnyView(
                    Button(toggleLabel) { revealed.toggle() }
                        .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                        .foregroundStyle(QorColor.accentBlue)
                )
            }
        )
    }
}

private extension NameFieldError {
    var message: String {
        switch self {
        case .required: return String(localized: "signup_error_name_required")
        }
    }
}

private extension EmailFieldError {
    var message: String {
        switch self {
        case .required: return String(localized: "signup_error_email_required")
        case .invalidFormat: return String(localized: "signup_error_email_invalid")
        }
    }
}

private extension PasswordFieldError {
    var message: String {
        switch self {
        case .required: return String(localized: "signup_error_password_required")
        }
    }
}

private extension ConfirmPasswordFieldError {
    var message: String {
        switch self {
        case .mismatch: return String(localized: "signup_error_confirm_password_mismatch")
        }
    }
}

private extension BirthdateFieldError {
    var message: String {
        switch self {
        case .required: return String(localized: "signup_error_birthdate_required")
        }
    }
}
