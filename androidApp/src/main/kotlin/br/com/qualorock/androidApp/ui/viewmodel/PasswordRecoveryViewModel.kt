package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.user.ConfirmResetResult
import domain.user.VerifyResetCodeResult
import domain.user.usecase.ResetPassword
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val OtpCodeLength = 6

/**
 * Basic client-side sanity floor for the new-password field — not `qor-api`'s actual
 * `config('qor.password_rules.min')` (not yet mirrored as a shared constant, same caveat as
 * [LoginViewModel]/[SignupViewModel]), just enough to catch an obviously-too-short entry before
 * a round trip. The server remains the source of truth and its message is shown verbatim on a
 * [ConfirmResetResult.Failure].
 */
private const val MinNewPasswordLength = 8

/** Which of the 3 steps of the wizard is showing — see [PasswordRecoveryViewModel] KDoc. */
sealed class PasswordRecoveryStep {
    data object RequestEmail : PasswordRecoveryStep()

    /** [email] is carried over from step 1 so step 2 doesn't need to ask for it again. */
    data class VerifyCode(val email: String) : PasswordRecoveryStep()

    /** [email] and the [token] returned by [ResetPassword.verifyResetCode] are carried into step 3. */
    data class NewPassword(val email: String, val token: String) : PasswordRecoveryStep()
}

/** Client-side validation failure for the new-password field (AUTH-13/AUTH-16). */
enum class NewPasswordFieldError {
    Required,
    TooShort,
}

/** One-shot outcome [PasswordRecoveryScreen] consumes exactly once. Nav-graph wiring happens in A14. */
sealed class PasswordRecoveryEvent {
    data object ResetSuccess : PasswordRecoveryEvent()
}

/** Form + submission state for [PasswordRecoveryScreen], across all 3 wizard steps. */
data class PasswordRecoveryUiState(
    val step: PasswordRecoveryStep = PasswordRecoveryStep.RequestEmail,
    val email: String = "",
    val emailError: EmailFieldError? = null,
    val code: String = "",
    val codeError: CodeFieldError? = null,
    val newPassword: String = "",
    val newPasswordError: NewPasswordFieldError? = null,
    val submitError: String? = null,
    val isLoading: Boolean = false,
)

/**
 * A10/A21 — form state + submit orchestration for `PasswordRecoveryScreen` (auth-fan-profile
 * AUTH-13–AUTH-16). Delegates to [ResetPassword] (thin wrapper over `UserRepository`'s 3-step
 * reset flow); owns client-side validation, loading/error UI state, the [PasswordRecoveryStep]
 * state machine, and the one-shot [events] the caller's nav graph wires up in A14.
 *
 * **Step 1 (AUTH-14 anti-enumeration).** [ResetPassword.requestReset] is fire-and-forget (`Unit`,
 * no server message) — same shape as [EmailVerificationViewModel]'s `VerifyEmail.resend`. So
 * [onSubmitEmail] always advances to [PasswordRecoveryStep.VerifyCode] and always shows the same
 * generic pt-BR confirmation copy, regardless of whether the email exists server-side; the screen
 * renders that copy whenever `step` is [PasswordRecoveryStep.VerifyCode].
 *
 * **Step 2 — code verification.** [onSubmitCode] calls [ResetPassword.verifyResetCode]. On
 * [VerifyResetCodeResult.Success] the returned token is stashed on [PasswordRecoveryStep.NewPassword]
 * and the wizard advances; on [VerifyResetCodeResult.Failure] the server's pt-BR message is shown
 * inline (`submitError`) and the fan stays on [PasswordRecoveryStep.VerifyCode] to retry.
 *
 * **Step 3 — new password.** [onSubmitNewPassword] calls [ResetPassword.confirmReset] with the
 * *token* obtained in step 2 (never the raw OTP code the fan typed) — matches `qor-website`'s
 * `app/recuperar-senha/page.tsx` 3-step wizard exactly (A21 retrofits A10's collapsed 2-step
 * stopgap to this shape).
 */
class PasswordRecoveryViewModel(private val resetPassword: ResetPassword) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    private val _events = Channel<PasswordRecoveryEvent>(Channel.BUFFERED)
    val events: Flow<PasswordRecoveryEvent> = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onCodeChange(value: String) {
        _uiState.update { it.copy(code = value, codeError = null, submitError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = null, submitError = null) }
    }

    fun onSubmitEmail() {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            resetPassword.requestReset(state.email)
            _uiState.update {
                it.copy(isLoading = false, step = PasswordRecoveryStep.VerifyCode(state.email))
            }
        }
    }

    fun onSubmitCode() {
        val state = _uiState.value
        val step = state.step
        if (step !is PasswordRecoveryStep.VerifyCode) return

        val codeError = validateCode(state.code)
        if (codeError != null) {
            _uiState.update { it.copy(codeError = codeError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, submitError = null) }
        viewModelScope.launch {
            when (val result = resetPassword.verifyResetCode(step.email, state.code)) {
                is VerifyResetCodeResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = PasswordRecoveryStep.NewPassword(step.email, result.token),
                        )
                    }
                }

                is VerifyResetCodeResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, submitError = result.message) }
                }
            }
        }
    }

    fun onSubmitNewPassword() {
        val state = _uiState.value
        val step = state.step
        if (step !is PasswordRecoveryStep.NewPassword) return

        val passwordError = validateNewPassword(state.newPassword)
        if (passwordError != null) {
            _uiState.update { it.copy(newPasswordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, submitError = null) }
        viewModelScope.launch {
            when (val result = resetPassword.confirmReset(step.email, step.token, state.newPassword)) {
                is ConfirmResetResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(PasswordRecoveryEvent.ResetSuccess)
                }

                is ConfirmResetResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, submitError = result.message) }
                }
            }
        }
    }

    private fun validateEmail(email: String): EmailFieldError? = when {
        email.isBlank() -> EmailFieldError.Required
        !EMAIL_REGEX.matches(email) -> EmailFieldError.InvalidFormat
        else -> null
    }

    private fun validateCode(code: String): CodeFieldError? = when {
        code.isBlank() -> CodeFieldError.Required
        code.length != OtpCodeLength -> CodeFieldError.InvalidLength
        else -> null
    }

    private fun validateNewPassword(password: String): NewPasswordFieldError? = when {
        password.isBlank() -> NewPasswordFieldError.Required
        password.length < MinNewPasswordLength -> NewPasswordFieldError.TooShort
        else -> null
    }

    private companion object {
        val EMAIL_REGEX = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
    }
}
