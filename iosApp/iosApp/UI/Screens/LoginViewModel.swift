import Foundation
import shared

/// Seam over `AuthenticateFan`'s exact (Kotlin-generated) completion-handler signature, so tests
/// can inject a fake rather than standing up the whole Koin graph. `AuthenticateFan` conforms
/// structurally via the extension below — no behavior change, just a testable abstraction.
protocol FanAuthenticating {
    func executeWithPassword(
        email: String,
        password: String,
        completionHandler: @escaping (LoginResult?, Error?) -> Void
    )
}

extension AuthenticateFan: FanAuthenticating {}

/// Client-side validation failures for the email field — see AUTH-11's generic-message rule for
/// why these stay separate from ``LoginSubmitError``. Mirrors Android's `EmailFieldError`.
enum EmailFieldError: Equatable {
    case required
    case invalidFormat
}

/// Client-side validation failure for the password field (non-empty only — see
/// ``LoginViewModel``'s doc comment for why password validation stays required-only here).
enum PasswordFieldError: Equatable {
    case required
}

/// The two server-distinguished failure branches of `LoginResult` this screen must tell apart
/// (AUTH-10/AUTH-11) — everything else about a failed submit collapses into these. Mirrors
/// Android's `LoginSubmitError`.
enum LoginSubmitError: Equatable {
    /// AUTH-11 — one generic message, never "wrong email" vs. "wrong password".
    case invalidCredentials
    /// AUTH-10 — account exists but isn't verified; carries the email onward to resend/verify.
    case unverifiedAccount(email: String)
}

/// Form + submission state for ``LoginView``.
struct LoginUiState: Equatable {
    var email: String = ""
    var password: String = ""
    var emailError: EmailFieldError?
    var passwordError: PasswordFieldError?
    var submitError: LoginSubmitError?
    var isLoading: Bool = false
}

/// I7 — form state + submit orchestration for `LoginView` (auth-fan-profile AUTH-09–AUTH-12).
/// Delegates the actual authentication call to `AuthenticateFan` (already wraps
/// `UserRepository.login` + session persistence via `SessionWriter`); this class owns only
/// client-side validation, loading/error UI state, and the navigation-callback closures the
/// caller wires up (nav-graph pushing is I14's job, not this screen's).
///
/// Password validation is required-only (non-empty): `qor-api`'s `config('qor.password_rules.min')`
/// is not yet mirrored as a client-exposed constant anywhere in `shared` — mirrors Android's
/// `LoginViewModel` doc comment on this exact point.
@MainActor
final class LoginViewModel: ObservableObject {
    @Published private(set) var uiState = LoginUiState()

    /// Called once, after a successful login.
    var onLoginSuccess: () -> Void = {}
    /// Called once, with the submitted email, when the account exists but isn't verified.
    var onNavigateToVerifyEmail: (String) -> Void = { _ in }

    private let authenticateFan: FanAuthenticating

    nonisolated init(authenticateFan: FanAuthenticating = IosDependencies.shared.authenticateFan()) {
        self.authenticateFan = authenticateFan
    }

    func onEmailChange(_ value: String) {
        uiState.email = value
        uiState.emailError = nil
        uiState.submitError = nil
    }

    func onPasswordChange(_ value: String) {
        uiState.password = value
        uiState.passwordError = nil
        uiState.submitError = nil
    }

    func onSubmit() {
        let emailError = Self.validateEmail(uiState.email)
        let passwordError = Self.validatePassword(uiState.password)

        if emailError != nil || passwordError != nil {
            uiState.emailError = emailError
            uiState.passwordError = passwordError
            return
        }

        let email = uiState.email
        let password = uiState.password
        uiState.isLoading = true
        uiState.submitError = nil

        Task {
            await submit(email: email, password: password)
        }
    }

    private func submit(email: String, password: String) async {
        let result: LoginResult
        do {
            result = try await performLogin(email: email, password: password)
        } catch {
            uiState.isLoading = false
            uiState.submitError = .invalidCredentials
            return
        }

        uiState.isLoading = false

        if result is LoginResult.Success {
            onLoginSuccess()
        } else if result is LoginResult.UnverifiedAccount {
            uiState.submitError = .unverifiedAccount(email: email)
            onNavigateToVerifyEmail(email)
        } else {
            // LoginResult.InvalidCredentials, and any future branch not yet distinguished by this
            // screen, collapse into the generic AUTH-11 message.
            uiState.submitError = .invalidCredentials
        }
    }

    private func performLogin(email: String, password: String) async throws -> LoginResult {
        try await withCheckedThrowingContinuation { continuation in
            authenticateFan.executeWithPassword(email: email, password: password) { result, error in
                if let result {
                    continuation.resume(returning: result)
                } else if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(throwing: NSError(domain: "LoginViewModel", code: -1))
                }
            }
        }
    }

    static func validateEmail(_ email: String) -> EmailFieldError? {
        if email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return .required
        }
        if !emailRegex.hasMatch(email) {
            return .invalidFormat
        }
        return nil
    }

    static func validatePassword(_ password: String) -> PasswordFieldError? {
        password.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .required : nil
    }

    private static let emailRegex = try! NSRegularExpression(pattern: "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
}

private extension NSRegularExpression {
    func hasMatch(_ string: String) -> Bool {
        firstMatch(in: string, range: NSRange(string.startIndex..., in: string)) != nil
    }
}
