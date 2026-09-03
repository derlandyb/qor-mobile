package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.user.LoginResult
import domain.user.usecase.AuthenticateFan
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Client-side validation failures for the email field — see AUTH-11's generic-message rule for why these stay separate from [LoginSubmitError]. */
enum class EmailFieldError {
    Required,
    InvalidFormat,
}

/** Client-side validation failure for the password field (non-empty only — see [LoginViewModel] KDoc). */
enum class PasswordFieldError {
    Required,
}

/**
 * The two server-distinguished failure branches of [LoginResult] this screen must tell apart
 * (AUTH-10/AUTH-11) — everything else about a failed submit collapses into these.
 */
sealed class LoginSubmitError {
    /** AUTH-11 — one generic message, never "wrong email" vs. "wrong password". */
    data object InvalidCredentials : LoginSubmitError()

    /** AUTH-10 — account exists but isn't verified; carries the email onward to resend/verify. */
    data class UnverifiedAccount(val email: String) : LoginSubmitError()
}

/** One-shot outcomes [LoginScreen] consumes exactly once. Nav-graph wiring happens in A14. */
sealed class LoginEvent {
    data object LoginSuccess : LoginEvent()
    data class NavigateToVerifyEmail(val email: String) : LoginEvent()
}

/** Form + submission state for [LoginScreen]. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: EmailFieldError? = null,
    val passwordError: PasswordFieldError? = null,
    val submitError: LoginSubmitError? = null,
    val isLoading: Boolean = false,
)

/**
 * A7 — form state + submit orchestration for `LoginScreen` (auth-fan-profile AUTH-09–AUTH-12).
 * Delegates the actual authentication call to [AuthenticateFan] (already wraps
 * `UserRepository.login` + session persistence via `SessionWriter`); this class owns only
 * client-side validation, loading/error UI state, and the one-shot navigation events the
 * caller's nav graph wires up in A14.
 *
 * Password validation is required-only (non-empty): `qor-api`'s `config('qor.password_rules.min')`
 * (currently 8) is not yet mirrored as a client-exposed constant anywhere in `shared`, and this
 * task's scope is `androidApp` only — inventing a duplicate constant here would violate the
 * no-magic-numbers rule as badly as inlining one. A future shared task should expose it so this
 * validation can be tightened without re-deriving the server's policy.
 */
class LoginViewModel(private val authenticateFan: AuthenticateFan) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, submitError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, submitError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)

        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, submitError = null) }
        viewModelScope.launch {
            when (authenticateFan.executeWithPassword(state.email, state.password)) {
                is LoginResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(LoginEvent.LoginSuccess)
                }

                is LoginResult.InvalidCredentials -> {
                    _uiState.update {
                        it.copy(isLoading = false, submitError = LoginSubmitError.InvalidCredentials)
                    }
                }

                is LoginResult.UnverifiedAccount -> {
                    val email = state.email
                    _uiState.update {
                        it.copy(isLoading = false, submitError = LoginSubmitError.UnverifiedAccount(email))
                    }
                    _events.send(LoginEvent.NavigateToVerifyEmail(email))
                }
            }
        }
    }

    private fun validateEmail(email: String): EmailFieldError? = when {
        email.isBlank() -> EmailFieldError.Required
        !EMAIL_REGEX.matches(email) -> EmailFieldError.InvalidFormat
        else -> null
    }

    private fun validatePassword(password: String): PasswordFieldError? =
        if (password.isBlank()) PasswordFieldError.Required else null

    private companion object {
        val EMAIL_REGEX = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
    }
}
