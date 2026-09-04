import SwiftUI
import shared

/// I10/A21 — which of the 3 steps of the password-recovery wizard is showing (email -> OTP
/// code -> new password; auth-fan-profile AUTH-13–AUTH-16). Mirrors Android's
/// `PasswordRecoveryStep` sealed class exactly — the fan-facing "success" screen is *not* a 4th
/// step here (kept a 1:1 mirror of Android's 3-case shape); it's tracked separately via
/// `PasswordRecoveryUiState.isSuccess` so [PasswordRecoveryView] can show a dedicated success
/// screen without complicating the wizard's own state machine.
enum PasswordRecoveryStep: Equatable {
    case requestEmail

    /// `email` is carried over from step 1 so step 2 doesn't need to ask for it again.
    case verifyCode(email: String)

    /// `email` and the `token` returned by `ResetPassword.verifyResetCode` are carried into step 3.
    case newPassword(email: String, token: String)
}

/// Client-side validation failure for the email field (AUTH-13/AUTH-16).
enum EmailFieldError: Equatable {
    case required
    case invalidFormat
}

/// Client-side validation failure for the OTP code field.
enum CodeFieldError: Equatable {
    case required
    case invalidLength
}

/// Client-side validation failure for the new-password field (AUTH-13/AUTH-16).
enum NewPasswordFieldError: Equatable {
    case required
    case tooShort
}

/// Form + submission state for [PasswordRecoveryView], across all 3 wizard steps plus the final
/// success screen.
struct PasswordRecoveryUiState: Equatable {
    var step: PasswordRecoveryStep = .requestEmail
    var email: String = ""
    var emailError: EmailFieldError?
    var code: String = ""
    var codeError: CodeFieldError?
    var newPassword: String = ""
    var newPasswordError: NewPasswordFieldError?
    var submitError: String?
    var isLoading: Bool = false
    var isSuccess: Bool = false
}

private let otpCodeLength = 6

/// Basic client-side sanity floor for the new-password field — not `qor-api`'s actual
/// `config('qor.password_rules.min')` (not yet mirrored as a shared constant, same caveat as
/// Android's `PasswordRecoveryViewModel`), just enough to catch an obviously-too-short entry
/// before a round trip. The server remains the source of truth and its message is shown verbatim
/// on a `ConfirmResetResult.Failure`.
private let minNewPasswordLength = 8

private let emailFormatPattern = #"^[^\s@]+@[^\s@]+\.[^\s@]+$"#

/// I10/A21 — form state + submit orchestration for [PasswordRecoveryView] (auth-fan-profile
/// AUTH-13–AUTH-16). Delegates to `ResetPassword` (thin wrapper over `UserRepository`'s 3-step
/// reset flow); owns client-side validation, loading/error UI state, the [PasswordRecoveryStep]
/// state machine, and the final success flag the view reacts to.
///
/// **Step 1 (AUTH-14 anti-enumeration).** `ResetPassword.requestReset` is fire-and-forget — so
/// ``onSubmitEmail()`` always advances to ``PasswordRecoveryStep/verifyCode(email:)`` and always
/// shows the same generic pt-BR confirmation copy, regardless of whether the email exists
/// server-side.
///
/// **Step 2 — code verification.** ``onSubmitCode()`` calls `ResetPassword.verifyResetCode`. On
/// `VerifyResetCodeResult.Success` the returned token is stashed on
/// ``PasswordRecoveryStep/newPassword(email:token:)`` and the wizard advances; on
/// `VerifyResetCodeResult.Failure` the server's pt-BR message is shown inline (`submitError`) and
/// the fan stays on ``PasswordRecoveryStep/verifyCode(email:)`` to retry.
///
/// **Step 3 — new password.** ``onSubmitNewPassword()`` calls `ResetPassword.confirmReset` with
/// the *token* obtained in step 2 (never the raw OTP code the fan typed). On success, `isSuccess`
/// flips to `true`; the view does not navigate itself — that's I14's job.
@MainActor
final class PasswordRecoveryViewModel: ObservableObject {
    @Published private(set) var uiState = PasswordRecoveryUiState()

    private let resetPassword: ResetPassword

    init(resetPassword: ResetPassword = IosDependencies.shared.resetPassword()) {
        self.resetPassword = resetPassword
    }

    func onEmailChange(_ value: String) {
        uiState.email = value
        uiState.emailError = nil
    }

    func onCodeChange(_ value: String) {
        uiState.code = sanitizeOtpInput(value)
        uiState.codeError = nil
        uiState.submitError = nil
    }

    func onNewPasswordChange(_ value: String) {
        uiState.newPassword = value
        uiState.newPasswordError = nil
        uiState.submitError = nil
    }

    func onSubmitEmail() async {
        let email = uiState.email
        if let error = validateEmail(email) {
            uiState.emailError = error
            return
        }

        uiState.isLoading = true
        _ = try? await resetPassword.requestReset(email: email)
        uiState.isLoading = false
        uiState.step = .verifyCode(email: email)
    }

    func onSubmitCode() async {
        guard case let .verifyCode(email) = uiState.step else { return }

        let code = uiState.code
        if let error = validateCode(code) {
            uiState.codeError = error
            return
        }

        uiState.isLoading = true
        uiState.submitError = nil
        do {
            let result = try await resetPassword.verifyResetCode(email: email, code: code)
            uiState.isLoading = false
            if let success = result as? VerifyResetCodeResult.Success {
                uiState.step = .newPassword(email: email, token: success.token)
            } else if let failure = result as? VerifyResetCodeResult.Failure {
                uiState.submitError = failure.message
            }
        } catch {
            uiState.isLoading = false
            uiState.submitError = String(localized: "error_generic_try_again")
        }
    }

    func onSubmitNewPassword() async {
        guard case let .newPassword(email, token) = uiState.step else { return }

        let newPassword = uiState.newPassword
        if let error = validateNewPassword(newPassword) {
            uiState.newPasswordError = error
            return
        }

        uiState.isLoading = true
        uiState.submitError = nil
        do {
            let result = try await resetPassword.confirmReset(email: email, token: token, newPassword: newPassword)
            uiState.isLoading = false
            if result is ConfirmResetResult.Success {
                uiState.isSuccess = true
            } else if let failure = result as? ConfirmResetResult.Failure {
                uiState.submitError = failure.message
            }
        } catch {
            uiState.isLoading = false
            uiState.submitError = String(localized: "error_generic_try_again")
        }
    }

    private func validateEmail(_ email: String) -> EmailFieldError? {
        if email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return .required
        }
        if email.range(of: emailFormatPattern, options: .regularExpression) == nil {
            return .invalidFormat
        }
        return nil
    }

    private func validateCode(_ code: String) -> CodeFieldError? {
        if code.isEmpty { return .required }
        if code.count != otpCodeLength { return .invalidLength }
        return nil
    }

    private func validateNewPassword(_ password: String) -> NewPasswordFieldError? {
        if password.isEmpty { return .required }
        if password.count < minNewPasswordLength { return .tooShort }
        return nil
    }
}

/// I10/A21 — password recovery, a 3-step wizard (email -> OTP code -> new password;
/// auth-fan-profile AUTH-13–AUTH-16) plus a final success screen. Mirrors Android's
/// `PasswordRecoveryScreen`/`qor-website`'s `app/recuperar-senha/page.tsx` UX. Owns only form +
/// submit UI: this view never pushes navigation itself — ``onResetSuccess`` (tapped from the
/// success screen) and ``onNavigateToLogin`` (the "Lembrou da senha?" link) are wired up by I14.
@MainActor
struct PasswordRecoveryView: View {
    @StateObject private var viewModel: PasswordRecoveryViewModel
    let onResetSuccess: () -> Void
    let onNavigateToLogin: () -> Void

    init(
        viewModel: PasswordRecoveryViewModel? = nil,
        onResetSuccess: @escaping () -> Void,
        onNavigateToLogin: @escaping () -> Void
    ) {
        _viewModel = StateObject(wrappedValue: viewModel ?? PasswordRecoveryViewModel())
        self.onResetSuccess = onResetSuccess
        self.onNavigateToLogin = onNavigateToLogin
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: QorSpace.space3) {
                Text(String(localized: "password_recovery_title"))
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextEventTitleLg.shared.SizeSp), weight: .bold))
                    .foregroundStyle(QorColor.textPrimary)

                if viewModel.uiState.isSuccess {
                    successContent
                } else {
                    stepContent

                    Text(String(localized: "password_recovery_link_login"))
                        .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                        .foregroundStyle(QorColor.accentBlue)
                        .padding(.top, QorSpace.space1)
                        .onTapGesture(perform: onNavigateToLogin)
                        .accessibilityIdentifier("password_recovery_login_link")
                }
            }
            .padding(QorSpace.space4)
        }
    }

    @ViewBuilder
    private var stepContent: some View {
        switch viewModel.uiState.step {
        case .requestEmail:
            requestEmailContent
        case .verifyCode:
            verifyCodeContent
        case .newPassword:
            newPasswordContent
        }
    }

    private var requestEmailContent: some View {
        VStack(alignment: .leading, spacing: QorSpace.space3) {
            Text(String(localized: "password_recovery_request_instructions"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)

            EmailField(
                value: Binding(get: { viewModel.uiState.email }, set: viewModel.onEmailChange),
                errorMessage: viewModel.uiState.emailError.map(emailErrorMessage)
            )

            PrimaryButton(
                text: String(localized: "cta_enviar_link_recuperacao"),
                onClick: { Task { await viewModel.onSubmitEmail() } },
                isLoading: viewModel.uiState.isLoading
            )
        }
        .accessibilityIdentifier("password_recovery_step_request_email")
    }

    private var verifyCodeContent: some View {
        VStack(alignment: .leading, spacing: QorSpace.space3) {
            Text(String(localized: "password_recovery_generic_confirmation"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)

            OtpCodeField(
                value: Binding(get: { viewModel.uiState.code }, set: viewModel.onCodeChange),
                errorMessage: viewModel.uiState.codeError.map(codeErrorMessage)
            )

            if let submitError = viewModel.uiState.submitError {
                Text(submitError)
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.danger)
            }

            PrimaryButton(
                text: String(localized: "cta_verificar_codigo"),
                onClick: { Task { await viewModel.onSubmitCode() } },
                isLoading: viewModel.uiState.isLoading
            )
        }
        .accessibilityIdentifier("password_recovery_step_verify_code")
    }

    private var newPasswordContent: some View {
        VStack(alignment: .leading, spacing: QorSpace.space3) {
            Text(String(localized: "password_recovery_new_password_instructions"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)

            PasswordField(
                value: Binding(get: { viewModel.uiState.newPassword }, set: viewModel.onNewPasswordChange),
                errorMessage: viewModel.uiState.newPasswordError.map(newPasswordErrorMessage)
            )

            if let submitError = viewModel.uiState.submitError {
                Text(submitError)
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.danger)
            }

            PrimaryButton(
                text: String(localized: "cta_redefinir_senha"),
                onClick: { Task { await viewModel.onSubmitNewPassword() } },
                isLoading: viewModel.uiState.isLoading
            )
        }
        .accessibilityIdentifier("password_recovery_step_new_password")
    }

    private var successContent: some View {
        VStack(alignment: .leading, spacing: QorSpace.space3) {
            Text(String(localized: "password_recovery_success_title"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp), weight: .semibold))
                .foregroundStyle(QorColor.textPrimary)

            Text(String(localized: "password_recovery_success_message"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)

            PrimaryButton(
                text: String(localized: "password_recovery_success_cta"),
                onClick: onResetSuccess
            )
        }
        .accessibilityIdentifier("password_recovery_step_success")
    }
}

private func emailErrorMessage(_ error: EmailFieldError) -> String {
    switch error {
    case .required: String(localized: "password_recovery_error_email_required")
    case .invalidFormat: String(localized: "password_recovery_error_email_invalid")
    }
}

private func codeErrorMessage(_ error: CodeFieldError) -> String {
    switch error {
    case .required: String(localized: "password_recovery_error_code_required")
    case .invalidLength: String(localized: "password_recovery_error_code_invalid_length")
    }
}

private func newPasswordErrorMessage(_ error: NewPasswordFieldError) -> String {
    switch error {
    case .required: String(localized: "password_recovery_error_password_required")
    case .tooShort: String(localized: "password_recovery_error_password_too_short")
    }
}
