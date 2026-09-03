package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.user.RegisterResult
import domain.user.usecase.RegisterFan
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Client-side validation failure for the name field (non-empty only). */
enum class NameFieldError {
    Required,
}

/** Client-side validation failure for the birthdate field (non-empty only — no format/age rule is invented here). */
enum class BirthdateFieldError {
    Required,
}

/** AUTH-02/AUTH-03 — consent must be explicitly accepted before an account can be created. */
enum class ConsentFieldError {
    Required,
}

/** One-shot outcome [SignupScreen] consumes exactly once. Nav-graph wiring happens in A14. */
sealed class SignupEvent {
    data class SignupSuccess(val email: String) : SignupEvent()
}

/** Server-side field keys `RegisterResult.Failure.fieldErrors` is known to use for this form. */
private const val FieldKeyName = "name"
private const val FieldKeyEmail = "email"
private const val FieldKeyPassword = "password"
private const val FieldKeyBirthdate = "birthdate"

private val EmailRegex = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

private fun validateName(name: String): NameFieldError? =
    if (name.isBlank()) NameFieldError.Required else null

private fun validateEmail(email: String): EmailFieldError? = when {
    email.isBlank() -> EmailFieldError.Required
    !EmailRegex.matches(email) -> EmailFieldError.InvalidFormat
    else -> null
}

private fun validatePassword(password: String): PasswordFieldError? =
    if (password.isBlank()) PasswordFieldError.Required else null

private fun validateBirthdate(birthdate: String): BirthdateFieldError? =
    if (birthdate.isBlank()) BirthdateFieldError.Required else null

private fun validateConsent(accepted: Boolean): ConsentFieldError? =
    if (!accepted) ConsentFieldError.Required else null

/** Form + submission state for [SignupScreen]. */
data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val birthdate: String = "",
    val consentAccepted: Boolean = false,
    val nameError: NameFieldError? = null,
    val emailError: EmailFieldError? = null,
    val passwordError: PasswordFieldError? = null,
    val birthdateError: BirthdateFieldError? = null,
    val consentError: ConsentFieldError? = null,
    val nameServerError: String? = null,
    val emailServerError: String? = null,
    val passwordServerError: String? = null,
    val birthdateServerError: String? = null,
    /** Fallback banner — used when a [RegisterResult.Failure] carries no known field key. */
    val submitError: String? = null,
    val isLoading: Boolean = false,
)

/**
 * A8 — form state + submit orchestration for `SignupScreen` (auth-fan-profile AUTH-01–AUTH-03).
 * Delegates account creation to [RegisterFan] (thin wrapper over `UserRepository.register`);
 * this class owns only client-side validation, loading/error UI state, and the one-shot
 * navigation event the caller's nav graph wires up in A14.
 *
 * Password validation is required-only (non-empty), same caveat as [LoginViewModel]:
 * `qor-api`'s `config('qor.password_rules.min')` is not yet mirrored as a client-exposed
 * constant, and this task's scope is `androidApp` only — inventing a duplicate constant here
 * would violate the no-magic-numbers rule as badly as inlining one.
 */
class SignupViewModel(private val registerFan: RegisterFan) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignupEvent>(Channel.BUFFERED)
    val events: Flow<SignupEvent> = _events.receiveAsFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, nameServerError = null, submitError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, emailServerError = null, submitError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(password = value, passwordError = null, passwordServerError = null, submitError = null)
        }
    }

    fun onBirthdateChange(value: String) {
        _uiState.update {
            it.copy(birthdate = value, birthdateError = null, birthdateServerError = null, submitError = null)
        }
    }

    fun onConsentChange(accepted: Boolean) {
        _uiState.update { it.copy(consentAccepted = accepted, consentError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val nameError = validateName(state.name)
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)
        val birthdateError = validateBirthdate(state.birthdate)
        val consentError = validateConsent(state.consentAccepted)

        val hasValidationError = listOf(nameError, emailError, passwordError, birthdateError, consentError)
            .any { it != null }
        if (hasValidationError) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    birthdateError = birthdateError,
                    consentError = consentError,
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, submitError = null) }
        viewModelScope.launch {
            val result = registerFan.execute(
                email = state.email,
                password = state.password,
                birthdate = state.birthdate,
                name = state.name,
                consentAccepted = state.consentAccepted,
            )
            when (result) {
                is RegisterResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(SignupEvent.SignupSuccess(state.email))
                }

                is RegisterResult.Failure -> applyFailure(result)
            }
        }
    }

    private fun applyFailure(result: RegisterResult.Failure) {
        val nameServerError = result.fieldErrors[FieldKeyName]?.firstOrNull()
        val emailServerError = result.fieldErrors[FieldKeyEmail]?.firstOrNull()
        val passwordServerError = result.fieldErrors[FieldKeyPassword]?.firstOrNull()
        val birthdateServerError = result.fieldErrors[FieldKeyBirthdate]?.firstOrNull()
        val matchedKnownField = listOf(nameServerError, emailServerError, passwordServerError, birthdateServerError)
            .any { it != null }

        _uiState.update {
            it.copy(
                isLoading = false,
                nameServerError = nameServerError,
                emailServerError = emailServerError,
                passwordServerError = passwordServerError,
                birthdateServerError = birthdateServerError,
                submitError = if (matchedKnownField) null else result.message,
            )
        }
    }
}
