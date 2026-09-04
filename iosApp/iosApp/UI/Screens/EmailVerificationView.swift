import SwiftUI
import shared

/// I9 — email-verification OTP entry (auth-fan-profile AUTH-01/AUTH-10; matches `qor-website`'s
/// shipped W20 `/verificar-email` and Android's `EmailVerificationScreen`). Owns only form +
/// submit + resend UI; it never pushes navigation itself — see
/// `EmailVerificationViewModel`'s doc for why `onVerifiedForSignup`/`onVerifiedForEmailChange`
/// are two distinct closures instead of one generic "success" callback.
struct EmailVerificationView: View {
    let email: String
    @StateObject private var viewModel: EmailVerificationViewModel

    init(
        email: String,
        context: EmailVerificationContext,
        onVerifiedForSignup: @escaping () -> Void,
        onVerifiedForEmailChange: @escaping () -> Void
    ) {
        self.email = email
        _viewModel = StateObject(
            wrappedValue: EmailVerificationViewModel(
                context: context,
                onVerifiedForSignup: onVerifiedForSignup,
                onVerifiedForEmailChange: onVerifiedForEmailChange
            )
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: QorSpace.space3) {
            Text(String(localized: "email_verification_title"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextEventTitleLg.shared.SizeSp), weight: .bold))
                .foregroundStyle(QorColor.textPrimary)

            Text(String(format: String(localized: "email_verification_instructions"), email))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)

            OtpCodeField(
                value: Binding(get: { viewModel.uiState.code }, set: { viewModel.onCodeChange($0) }),
                errorMessage: viewModel.uiState.codeError?.message
            )

            if let submitError = viewModel.uiState.submitError {
                Text(submitError)
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.danger)
            }

            PrimaryButton(
                text: String(localized: "cta_verificar_codigo"),
                onClick: { Task { await viewModel.onSubmit(email: email) } },
                isLoading: viewModel.uiState.isLoading
            )

            HStack(spacing: QorSpace.space1) {
                Text(String(localized: "email_verification_resend_prompt"))
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.textSecondary)

                if viewModel.uiState.cooldown.canResend {
                    Text(String(localized: "cta_reenviar"))
                        .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                        .foregroundStyle(QorColor.accentBlue)
                        .onTapGesture { Task { await viewModel.onResend(email: email) } }
                } else {
                    Text(
                        String(
                            format: String(localized: "email_verification_resend_countdown"),
                            viewModel.uiState.cooldown.remainingSeconds
                        )
                    )
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.textTertiary)
                }
            }

            if viewModel.uiState.resendConfirmation {
                Text(String(localized: "email_verification_resend_confirmation"))
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.textSecondary)
            }
        }
        .padding(QorSpace.space4)
    }
}
