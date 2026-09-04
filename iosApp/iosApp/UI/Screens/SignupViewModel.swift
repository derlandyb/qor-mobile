import Foundation
import shared

// MARK: - Field errors (client-side validation only; mirrors Android's `SignupViewModel`)

/// Client-side validation failure for the name field (non-empty only).
enum NameFieldError {
    case required
}

/// Client-side validation failure for the email field.
enum SignupEmailFieldError {
    case required
    case invalidFormat
}

/// Client-side validation failure for the password field (non-empty only).
enum SignupPasswordFieldError {
    case required
}

/// I8's password-confirmation step (`qor-website`'s `cadastro/page.tsx` has this; Android's A8
/// shipped without it despite its own task title naming it — see this file's `SignupViewModel`
/// doc for the deviation note). Mirrors the website's single mismatch message: no separate
/// "required" case, a blank confirmation field just fails to match a non-blank password.
enum ConfirmPasswordFieldError {
    case mismatch
}

/// Client-side validation failure for the birthdate field (non-empty only — no format/age rule
/// is invented here).
enum BirthdateFieldError {
    case required
}

/// AUTH-02/AUTH-03 — consent must be explicitly accepted before an account can be created.
enum ConsentFieldError {
    case required
}

private let emailRegexPattern = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"

private func validateName(_ name: String) -> NameFieldError? {
    name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .required : nil
}

private func validateEmail(_ email: String) -> SignupEmailFieldError? {
    if email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
        return .required
    }
    guard let regex = try? NSRegularExpression(pattern: emailRegexPattern) else { return .invalidFormat }
    let range = NSRange(email.startIndex..<email.endIndex, in: email)
    return regex.firstMatch(in: email, options: [], range: range) != nil ? nil : .invalidFormat
}

private func validatePassword(_ password: String) -> SignupPasswordFieldError? {
    password.isEmpty ? .required : nil
}

private func validateConfirmPassword(password: String, confirmPassword: String) -> ConfirmPasswordFieldError? {
    password == confirmPassword ? nil : .mismatch
}

private func validateBirthdate(_ birthdate: String) -> BirthdateFieldError? {
    birthdate.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .required : nil
}

private func validateConsent(_ accepted: Bool) -> ConsentFieldError? {
    accepted ? nil : .required
}

/// Server-side field keys `RegisterResultFailure.fieldErrors` is known to use for this form.
private let fieldKeyName = "name"
private let fieldKeyEmail = "email"
private let fieldKeyPassword = "password"
private let fieldKeyBirthdate = "birthdate"

/// Form + submission state for `SignupView`.
struct SignupUiState {
    var name = ""
    var email = ""
    var password = ""
    var confirmPassword = ""
    var birthdate = ""
    var consentAccepted = false

    var nameError: NameFieldError?
    var emailError: SignupEmailFieldError?
    var passwordError: SignupPasswordFieldError?
    var confirmPasswordError: ConfirmPasswordFieldError?
    var birthdateError: BirthdateFieldError?
    var consentError: ConsentFieldError?

    var nameServerError: String?
    var emailServerError: String?
    var passwordServerError: String?
    var birthdateServerError: String?

    /// Fallback banner — used when a `RegisterResultFailure` carries no known field key.
    var submitError: String?
    var isLoading = false
}

/// Narrow, Swift-idiomatic seam over `RegisterFan` (rather than depending on the concrete Kotlin
/// class directly), so tests can inject a fake without hand-rolling a full `UserRepository` fake
/// for the twelve unrelated methods `RegisterFan`'s real backing `UserRepository` also declares.
protocol SignupRegistering {
    func execute(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Bool
    ) async throws -> RegisterResult
}

extension RegisterFan: SignupRegistering {}

/**
 * I8 — form state + submit orchestration for `SignupView` (auth-fan-profile AUTH-01–AUTH-05,
 * plus a password-confirmation step). Delegates account creation to `RegisterFan` (thin wrapper
 * over `UserRepository.register`); this class owns only client-side validation, loading/error UI
 * state, and calling `onSignupSuccess` once on success — no navigation destination is pushed
 * here, that's I14's job (mirrors Android's `SignupScreen`/`SignupViewModel` split).
 *
 * **Deviation from Android's A8**: `androidApp`'s shipped `SignupScreen`/`SignupViewModel` have
 * no password-confirmation field at all, despite A8's own task title naming one — a pre-existing
 * gap in the Android reference, not a deliberate design decision (confirmed by reading
 * `SignupViewModel.kt`/`SignupViewModelTest.kt` in full: no `confirmPassword` state, no mismatch
 * validation). I8's task explicitly calls for the step, and `qor-website`'s `cadastro/page.tsx`
 * already implements it (`password !== confirmPassword` → "As senhas não coincidem."), so this
 * class follows the website's validation shape rather than transliterating Android's gap.
 *
 * Password validation is otherwise required-only (non-empty), same caveat as Android's
 * `SignupViewModel`: `qor-api`'s `config('qor.password_rules.min')` isn't mirrored as a
 * client-exposed constant yet, and this task's scope is `iosApp` only.
 */
@MainActor
final class SignupViewModel: ObservableObject {
    @Published private(set) var uiState = SignupUiState()

    private let registerFan: SignupRegistering
    private let onSignupSuccess: (String) -> Void

    init(
        registerFan: SignupRegistering = IosDependencies.shared.registerFan(),
        onSignupSuccess: @escaping (String) -> Void
    ) {
        self.registerFan = registerFan
        self.onSignupSuccess = onSignupSuccess
    }

    func onNameChange(_ value: String) {
        uiState.name = value
        uiState.nameError = nil
        uiState.nameServerError = nil
        uiState.submitError = nil
    }

    func onEmailChange(_ value: String) {
        uiState.email = value
        uiState.emailError = nil
        uiState.emailServerError = nil
        uiState.submitError = nil
    }

    func onPasswordChange(_ value: String) {
        uiState.password = value
        uiState.passwordError = nil
        uiState.passwordServerError = nil
        uiState.confirmPasswordError = nil
        uiState.submitError = nil
    }

    func onConfirmPasswordChange(_ value: String) {
        uiState.confirmPassword = value
        uiState.confirmPasswordError = nil
        uiState.submitError = nil
    }

    func onBirthdateChange(_ value: String) {
        uiState.birthdate = value
        uiState.birthdateError = nil
        uiState.birthdateServerError = nil
        uiState.submitError = nil
    }

    func onConsentChange(_ accepted: Bool) {
        uiState.consentAccepted = accepted
        uiState.consentError = nil
    }

    func submit() async {
        let nameError = validateName(uiState.name)
        let emailError = validateEmail(uiState.email)
        let passwordError = validatePassword(uiState.password)
        let confirmPasswordError = validateConfirmPassword(password: uiState.password, confirmPassword: uiState.confirmPassword)
        let birthdateError = validateBirthdate(uiState.birthdate)
        let consentError = validateConsent(uiState.consentAccepted)

        let hasValidationError = nameError != nil || emailError != nil || passwordError != nil
            || confirmPasswordError != nil || birthdateError != nil || consentError != nil

        if hasValidationError {
            uiState.nameError = nameError
            uiState.emailError = emailError
            uiState.passwordError = passwordError
            uiState.confirmPasswordError = confirmPasswordError
            uiState.birthdateError = birthdateError
            uiState.consentError = consentError
            return
        }

        uiState.isLoading = true
        uiState.submitError = nil

        let email = uiState.email
        do {
            let result = try await registerFan.execute(
                email: email,
                password: uiState.password,
                birthdate: uiState.birthdate,
                name: uiState.name,
                consentAccepted: uiState.consentAccepted
            )
            apply(result: result, email: email)
        } catch {
            uiState.isLoading = false
            uiState.submitError = error.localizedDescription
        }
    }

    private func apply(result: RegisterResult, email: String) {
        switch result {
        case is RegisterResult.Success:
            uiState.isLoading = false
            onSignupSuccess(email)
        case let failure as RegisterResult.Failure:
            applyFailure(failure)
        default:
            // Exhaustive per `RegisterResult`'s two Kotlin subclasses — this branch is
            // unreachable in practice, kept only so a future new subclass fails loudly
            // (surfaced via `submitError`) rather than silently dropping the loading state.
            uiState.isLoading = false
            uiState.submitError = nil
        }
    }

    private func applyFailure(_ result: RegisterResult.Failure) {
        let nameServerError = result.fieldErrors[fieldKeyName]?.first
        let emailServerError = result.fieldErrors[fieldKeyEmail]?.first
        let passwordServerError = result.fieldErrors[fieldKeyPassword]?.first
        let birthdateServerError = result.fieldErrors[fieldKeyBirthdate]?.first
        let matchedKnownField = nameServerError != nil || emailServerError != nil
            || passwordServerError != nil || birthdateServerError != nil

        uiState.isLoading = false
        uiState.nameServerError = nameServerError
        uiState.emailServerError = emailServerError
        uiState.passwordServerError = passwordServerError
        uiState.birthdateServerError = birthdateServerError
        uiState.submitError = matchedKnownField ? nil : result.message
    }
}
