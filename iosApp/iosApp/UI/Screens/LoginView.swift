import SwiftUI
import shared

/// I7 — returning-fan login (auth-fan-profile AUTH-06–AUTH-12; Stitch screen
/// `cfa5690fed3d487897d65de249ad7f1d`). Owns only form + submit UI: on success it calls
/// `onLoginSuccess`, on an unverified account it calls `onNavigateToVerifyEmail` with the
/// submitted email, on the forgot-password link it calls `onNavigateToPasswordRecovery`, and on
/// the signup link it calls `onNavigateToSignup` — actually pushing a navigation destination is
/// I14's nav-graph job, not this screen's (mirrors Android's `LoginScreen`).
///
/// **"Entrar com Google" is a deliberate disabled stub.** No Google Sign-In SDK is wired into
/// `iosApp` yet — this button renders per the Stitch design but is inert (`enabled: false`,
/// no-op action) rather than fabricating an OAuth flow. Wiring it up is a separate, not-yet-
/// scheduled task.
struct LoginView: View {
    @StateObject private var viewModel: LoginViewModel

    @MainActor
    init(
        onLoginSuccess: @escaping () -> Void,
        onNavigateToVerifyEmail: @escaping (String) -> Void,
        onNavigateToSignup: @escaping () -> Void,
        onNavigateToPasswordRecovery: @escaping () -> Void,
        viewModel: LoginViewModel = LoginViewModel()
    ) {
        self.onNavigateToSignup = onNavigateToSignup
        self.onNavigateToPasswordRecovery = onNavigateToPasswordRecovery
        viewModel.onLoginSuccess = onLoginSuccess
        viewModel.onNavigateToVerifyEmail = onNavigateToVerifyEmail
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    let onNavigateToSignup: () -> Void
    let onNavigateToPasswordRecovery: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: QorSpace.space3) {
            Text(String(localized: "login_title"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextEventTitleLg.shared.SizeSp), weight: .bold))
                .foregroundStyle(QorColor.textPrimary)

            EmailField(
                value: Binding(
                    get: { viewModel.uiState.email },
                    set: { viewModel.onEmailChange($0) }
                ),
                errorMessage: viewModel.uiState.emailError.map { $0.localizedMessage }
            )

            PasswordField(
                value: Binding(
                    get: { viewModel.uiState.password },
                    set: { viewModel.onPasswordChange($0) }
                ),
                errorMessage: viewModel.uiState.passwordError.map { $0.localizedMessage }
            )

            if let submitError = viewModel.uiState.submitError {
                Text(submitError.localizedMessage)
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.danger)
                    .id("login_submit_error")
            }

            PrimaryButton(
                text: String(localized: "cta_entrar"),
                onClick: { viewModel.onSubmit() },
                isLoading: viewModel.uiState.isLoading
            )

            SecondaryButton(
                text: String(localized: "cta_entrar_com_google"),
                onClick: {},
                enabled: false
            )
            .id("login_google_stub")

            Text(String(localized: "login_link_password_recovery"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                .foregroundStyle(QorColor.accentBlue)
                .padding(.top, QorSpace.space1)
                .onTapGesture(perform: onNavigateToPasswordRecovery)
                .id("login_link_password_recovery")

            Text(String(localized: "login_link_signup"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                .foregroundStyle(QorColor.accentBlue)
                .onTapGesture(perform: onNavigateToSignup)
                .id("login_link_signup")
        }
        .padding(QorSpace.space4)
    }
}

private extension EmailFieldError {
    var localizedMessage: String {
        switch self {
        case .required: return String(localized: "login_error_email_required")
        case .invalidFormat: return String(localized: "login_error_email_invalid")
        }
    }
}

private extension PasswordFieldError {
    var localizedMessage: String {
        switch self {
        case .required: return String(localized: "login_error_password_required")
        }
    }
}

private extension LoginSubmitError {
    var localizedMessage: String {
        switch self {
        case .invalidCredentials: return String(localized: "login_error_invalid_credentials")
        case .unverifiedAccount: return String(localized: "login_error_unverified_account")
        }
    }
}
