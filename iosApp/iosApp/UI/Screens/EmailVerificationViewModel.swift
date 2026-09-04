import Foundation
import shared

/// I9 — client-side validation for the 6-digit OTP field, non-empty + exact length only
/// (AUTH-10). Mirrors Android's `CodeFieldError`.
enum CodeFieldError: Equatable {
    case required
    case invalidLength

    var message: String {
        switch self {
        case .required:
            return String(localized: "email_verification_error_code_required")
        case .invalidLength:
            return String(localized: "email_verification_error_code_invalid_length")
        }
    }
}

/// Which flow launched `EmailVerificationView` — the real endpoint issues no session on
/// verify (see [EmailVerificationViewModel]'s doc), so where `onVerified` should navigate
/// differs by caller: back to Login after signup, back to Profile after an email-change.
/// Nav-graph state, like `email` — not owned by this screen's own state.
enum EmailVerificationContext {
    case signup
    case emailChange
}

/// Pure resend-cooldown state machine, mirroring Android's `VerificationCooldown` — a plain
/// value type so ticking is unit-testable without any async waiting.
struct VerificationCooldown: Equatable {
    var remainingSeconds: Int

    /// True once the countdown has fully elapsed — the resend action becomes tappable.
    var canResend: Bool { remainingSeconds <= 0 }

    func ticked() -> VerificationCooldown {
        VerificationCooldown(remainingSeconds: max(remainingSeconds - 1, 0))
    }

    static func started(
        totalSeconds: Int = Int(QorConfig.shared.EmailVerificationResendCooldownSeconds)
    ) -> VerificationCooldown {
        VerificationCooldown(remainingSeconds: totalSeconds)
    }
}

/// Form + submission state for `EmailVerificationView`.
struct EmailVerificationUiState: Equatable {
    var code: String = ""
    var codeError: CodeFieldError?
    var submitError: String?
    var isLoading: Bool = false
    var cooldown: VerificationCooldown = .started()
    var resendConfirmation: Bool = false
}

/// Narrow protocol over `shared`'s `VerifyEmail` use case so tests can substitute a fake
/// without touching Koin — `VerifyEmail` itself is `final` on the Kotlin/Native side (can't be
/// subclassed from Swift), but its designated initializer takes the `UserRepository` protocol,
/// so a real `VerifyEmail(userRepository: fake)` built from a fake repository conforms to this
/// exactly the same way Android's `EmailVerificationViewModelTest` builds a real `VerifyEmail`
/// over a `FakeVerifyEmailUserRepository`.
protocol EmailVerifying {
    func resend(email: String) async throws
    func verifyCode(email: String, code: String) async throws -> VerifyEmailResult
}

extension VerifyEmail: EmailVerifying {}

private let otpCodeLength = 6

/**
 * I9 — form state + submit orchestration for `EmailVerificationView` (AUTH-01/AUTH-10).
 * Delegates to `VerifyEmail` (S9's thin wrapper over `UserRepository`); owns client-side OTP
 * validation, loading/error UI state, and the `VerificationCooldown` driving the resend button.
 *
 * **Verifying does NOT log the fan in.** Per `VerifyEmailResult`'s KDoc, `verifyEmailCode` only
 * marks the account verified — it returns no session/token — so this must never navigate
 * straight to Home. Which destination it *does* navigate to depends on which flow launched this
 * screen (`context`, passed in like `email` as nav-graph state, not this class's own): signup
 * routes to Login (matching `qor-website`'s shipped W20 behavior), an email-change routes back
 * to Profile. [onVerifiedForSignup]/[onVerifiedForEmailChange] are therefore two distinct
 * closures rather than one generic "success" callback, so I14's nav-graph wiring can tell them
 * apart — this view never pushes navigation itself.
 *
 * `resend` is fire-and-forget (no server message) — a resend tap always shows a static local
 * pt-BR confirmation string and restarts the cooldown; a failure is swallowed rather than shown,
 * matching Android's `EmailVerificationViewModel.onResend`.
 */
@MainActor
final class EmailVerificationViewModel: ObservableObject {
    @Published private(set) var uiState = EmailVerificationUiState()

    private let context: EmailVerificationContext
    private let verifyEmail: EmailVerifying
    private let onVerifiedForSignup: () -> Void
    private let onVerifiedForEmailChange: () -> Void
    private var cooldownTask: Task<Void, Never>?

    init(
        context: EmailVerificationContext,
        verifyEmail: EmailVerifying = IosDependencies.shared.verifyEmail(),
        onVerifiedForSignup: @escaping () -> Void,
        onVerifiedForEmailChange: @escaping () -> Void
    ) {
        self.context = context
        self.verifyEmail = verifyEmail
        self.onVerifiedForSignup = onVerifiedForSignup
        self.onVerifiedForEmailChange = onVerifiedForEmailChange
        startCooldown()
    }

    deinit {
        cooldownTask?.cancel()
    }

    func onCodeChange(_ value: String) {
        uiState.code = value
        uiState.codeError = nil
        uiState.submitError = nil
    }

    func onSubmit(email: String) async {
        if let codeError = Self.validate(code: uiState.code) {
            uiState.codeError = codeError
            return
        }

        uiState.isLoading = true
        uiState.submitError = nil
        let code = uiState.code

        do {
            let result = try await verifyEmail.verifyCode(email: email, code: code)
            uiState.isLoading = false
            if result is VerifyEmailResult.Success {
                switch context {
                case .signup:
                    onVerifiedForSignup()
                case .emailChange:
                    onVerifiedForEmailChange()
                }
            } else if let failure = result as? VerifyEmailResult.Failure {
                uiState.submitError = failure.message
            }
        } catch {
            uiState.isLoading = false
            uiState.submitError = error.localizedDescription
        }
    }

    func onResend(email: String) async {
        guard uiState.cooldown.canResend else { return }
        try? await verifyEmail.resend(email: email)
        uiState.resendConfirmation = true
        startCooldown()
    }

    private func startCooldown() {
        cooldownTask?.cancel()
        uiState.cooldown = .started()
        cooldownTask = Task { [weak self] in
            while let self, self.uiState.cooldown.remainingSeconds > 0 {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                if Task.isCancelled { return }
                self.uiState.cooldown = self.uiState.cooldown.ticked()
            }
        }
    }

    /// Test-only escape hatch so `onResend`'s cooldown-gated path is unit-testable without a
    /// real `Task.sleep` wait — mirrors Android's `EmailVerificationViewModelTest` using a fake
    /// clock/dispatcher instead.
    func forceCooldownElapsedForTesting() {
        cooldownTask?.cancel()
        uiState.cooldown = VerificationCooldown(remainingSeconds: 0)
    }

    private static func validate(code: String) -> CodeFieldError? {
        if code.isEmpty { return .required }
        if code.count != otpCodeLength { return .invalidLength }
        return nil
    }
}
