package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.QorConfig
import domain.user.VerifyEmailResult
import domain.user.usecase.VerifyEmail
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MillisPerSecond = 1_000L
private const val OtpCodeLength = 6

/** Client-side validation for the 6-digit OTP field — non-empty + exact length only (AUTH-10). */
enum class CodeFieldError {
    Required,
    InvalidLength,
}

/** One-shot outcome [EmailVerificationScreen] consumes exactly once. Nav-graph wiring happens in A14. */
sealed class EmailVerificationEvent {
    data object Verified : EmailVerificationEvent()
}

/**
 * Pure resend-cooldown state machine — [br.com.qualorock.androidApp.ui.components.CtaButtonMotion]-style
 * pure function so ticking is unit-testable without Compose or a real `delay()`.
 * [EmailVerificationViewModel] drives it with a `viewModelScope` coroutine ticker; [tick] and
 * [started] carry no side effects of their own.
 */
data class VerificationCooldown(val remainingSeconds: Long) {
    /** True once the countdown has fully elapsed — the resend action becomes tappable. */
    val canResend: Boolean get() = remainingSeconds <= 0

    fun tick(): VerificationCooldown = VerificationCooldown((remainingSeconds - 1).coerceAtLeast(0))

    companion object {
        fun started(totalSeconds: Long = QorConfig.EmailVerificationResendCooldownSeconds): VerificationCooldown =
            VerificationCooldown(totalSeconds)
    }
}

/** Form + submission state for [EmailVerificationScreen]. */
data class EmailVerificationUiState(
    val code: String = "",
    val codeError: CodeFieldError? = null,
    val submitError: String? = null,
    val isLoading: Boolean = false,
    val cooldown: VerificationCooldown = VerificationCooldown.started(),
    val resendConfirmation: Boolean = false,
)

/**
 * A9 — form state + submit orchestration for `EmailVerificationScreen` (AUTH-01/AUTH-10).
 * Delegates to [VerifyEmail] (S12b's thin wrapper over `UserRepository`); owns client-side OTP
 * validation, loading/error UI state, the [VerificationCooldown] ticker driving the resend
 * button, and the one-shot [events] the caller's nav graph wires up in A14.
 *
 * The `email` this screen verifies is nav-graph state (A14), not this class's own — [onSubmit]
 * and [onResend] take it as a parameter, matching the DI registration `EmailVerificationViewModel(get())`
 * in `AppModule.kt` (only [VerifyEmail] is Koin-injected).
 *
 * **Verifying does NOT log the fan in.** Per [VerifyEmailResult]'s KDoc, `verifyEmailCode` only
 * marks the account verified — it returns no session/token — so [EmailVerificationEvent.Verified]
 * must be wired (A14) to navigate to Login, never straight to Home; this matches `qor-website`'s
 * shipped W20 behavior (see `verificar-email/page.tsx`, which links to `/entrar` after success).
 *
 * [VerifyEmail.resend] is fire-and-forget (`Unit`, no server message) — a resend tap always shows
 * a static local pt-BR confirmation string and restarts the cooldown, it never expects text back
 * from the use case.
 */
class EmailVerificationViewModel(private val verifyEmail: VerifyEmail) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private val _events = Channel<EmailVerificationEvent>(Channel.BUFFERED)
    val events: Flow<EmailVerificationEvent> = _events.receiveAsFlow()

    private var cooldownJob: Job? = null

    init {
        startCooldown()
    }

    fun onCodeChange(value: String) {
        _uiState.update { it.copy(code = value, codeError = null, submitError = null) }
    }

    fun onSubmit(email: String) {
        val state = _uiState.value
        val codeError = validateCode(state.code)
        if (codeError != null) {
            _uiState.update { it.copy(codeError = codeError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, submitError = null) }
        viewModelScope.launch {
            when (val result = verifyEmail.verifyCode(email, state.code)) {
                is VerifyEmailResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(EmailVerificationEvent.Verified)
                }

                is VerifyEmailResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, submitError = result.message) }
                }
            }
        }
    }

    fun onResend(email: String) {
        if (!_uiState.value.cooldown.canResend) return
        viewModelScope.launch {
            verifyEmail.resend(email)
            _uiState.update { it.copy(resendConfirmation = true) }
            startCooldown()
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        _uiState.update { it.copy(cooldown = VerificationCooldown.started()) }
        cooldownJob = viewModelScope.launch {
            while (_uiState.value.cooldown.remainingSeconds > 0) {
                delay(MillisPerSecond)
                _uiState.update { it.copy(cooldown = it.cooldown.tick()) }
            }
        }
    }

    private fun validateCode(code: String): CodeFieldError? = when {
        code.isBlank() -> CodeFieldError.Required
        code.length != OtpCodeLength -> CodeFieldError.InvalidLength
        else -> null
    }

    override fun onCleared() {
        cooldownJob?.cancel()
    }
}
