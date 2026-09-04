package br.com.qualorock.androidApp.ui.viewmodel

import data.QorConfig
import domain.enum.ConsentType
import domain.user.ConfirmResetResult
import domain.user.DataRightResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import domain.user.User
import domain.user.UserRepository
import domain.user.VerifyEmailResult
import domain.user.usecase.VerifyEmail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Minimal in-file [UserRepository] fake, same shape as `LoginViewModelTest`'s — keeps this test
 * independent of Koin and of `shared`'s test-only fakes (not visible cross-module).
 */
private class FakeVerifyEmailUserRepository(private val verifyResult: VerifyEmailResult) : UserRepository {
    var lastVerifyEmail: String? = null
    var lastVerifyCode: String? = null
    var resendCallCount: Int = 0
    var lastResendEmail: String? = null

    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = error("not used by EmailVerificationViewModelTest")

    override suspend fun login(email: String, password: String): LoginResult =
        error("not used by EmailVerificationViewModelTest")

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult =
        error("not used by EmailVerificationViewModelTest")

    override suspend fun logout() = Unit

    override suspend fun requestPasswordReset(email: String) = Unit

    override suspend fun verifyResetCode(email: String, code: String): domain.user.VerifyResetCodeResult =
        error("not used by EmailVerificationViewModelTest")

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult =
        error("not used by EmailVerificationViewModelTest")

    override suspend fun resendVerification(email: String) {
        resendCallCount += 1
        lastResendEmail = email
    }

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult {
        lastVerifyEmail = email
        lastVerifyCode = code
        return verifyResult
    }

    override suspend fun getProfile(): User = error("not used by EmailVerificationViewModelTest")

    override suspend fun updateProfile(fields: ProfileUpdateFields): User =
        error("not used by EmailVerificationViewModelTest")

    override suspend fun accessData(): DataRightResult = error("not used by EmailVerificationViewModelTest")
    override suspend fun exportData(): DataRightResult = error("not used by EmailVerificationViewModelTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by EmailVerificationViewModelTest")

    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        error("not used by EmailVerificationViewModelTest")
}

private fun sampleUser() = User(
    id = "u1",
    name = "Ana",
    email = "ana@example.com",
    emailVerifiedAt = "2026-01-01T00:00:00Z",
    phone = null,
    profilePictureUrl = null,
    birthdate = "1990-01-01",
)

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        verifyResult: VerifyEmailResult = VerifyEmailResult.Failure("n/a"),
    ): Pair<EmailVerificationViewModel, FakeVerifyEmailUserRepository> {
        val repository = FakeVerifyEmailUserRepository(verifyResult)
        return EmailVerificationViewModel(VerifyEmail(repository)) to repository
    }

    @Test
    fun `GIVEN it is constructed THEN the cooldown starts at the configured resend interval`() = runTest {
        val (vm, _) = viewModel()

        assertEquals(QorConfig.EmailVerificationResendCooldownSeconds, vm.uiState.value.cooldown.remainingSeconds)
        assertFalse(vm.uiState.value.cooldown.canResend)
    }

    @Test
    fun `GIVEN an empty code WHEN onSubmit is called THEN a required field error is set`() = runTest {
        val (vm, _) = viewModel()

        vm.onSubmit("ana@example.com")

        assertEquals(CodeFieldError.Required, vm.uiState.value.codeError)
    }

    @Test
    fun `GIVEN a code shorter than 6 digits WHEN onSubmit is called THEN an invalid-length error is set`() = runTest {
        val (vm, _) = viewModel()
        vm.onCodeChange("123")

        vm.onSubmit("ana@example.com")

        assertEquals(CodeFieldError.InvalidLength, vm.uiState.value.codeError)
    }

    @Test
    fun `GIVEN a code was invalid WHEN it is edited again THEN its error is cleared`() = runTest {
        val (vm, _) = viewModel()
        vm.onSubmit("ana@example.com")
        assertEquals(CodeFieldError.Required, vm.uiState.value.codeError)

        vm.onCodeChange("1")

        assertNull(vm.uiState.value.codeError)
    }

    @Test
    fun `GIVEN a valid code WHEN the use case returns Success THEN a Verified event is emitted`() = runTest {
        val (vm, repository) = viewModel(VerifyEmailResult.Success(sampleUser()))
        vm.onCodeChange("123456")

        vm.onSubmit("ana@example.com")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(EmailVerificationEvent.Verified, vm.events.first())
        assertEquals("ana@example.com", repository.lastVerifyEmail)
        assertEquals("123456", repository.lastVerifyCode)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.submitError)
    }

    @Test
    fun `GIVEN a valid code WHEN the use case returns Failure THEN the server message is shown as an inline error`() =
        runTest {
            val (vm, _) = viewModel(VerifyEmailResult.Failure("Código inválido ou expirado."))
            vm.onCodeChange("123456")

            vm.onSubmit("ana@example.com")
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals("Código inválido ou expirado.", vm.uiState.value.submitError)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `GIVEN the cooldown has not elapsed WHEN onResend is called THEN it is ignored`() = runTest {
        val (vm, repository) = viewModel()

        vm.onResend("ana@example.com")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, repository.resendCallCount)
    }

    @Test
    fun `GIVEN the cooldown has elapsed WHEN onResend is called THEN resend fires and the cooldown restarts`() = runTest {
        val (vm, repository) = viewModel()
        dispatcher.scheduler.advanceTimeBy(QorConfig.EmailVerificationResendCooldownSeconds * 1_000 + 1)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.cooldown.canResend)

        vm.onResend("ana@example.com")
        // runCurrent (not advanceUntilIdle) — the restarted cooldown's own 60-tick loop must
        // stay pending, not run to completion, or this assertion could never observe "just restarted".
        dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.resendCallCount)
        assertEquals("ana@example.com", repository.lastResendEmail)
        assertTrue(vm.uiState.value.resendConfirmation)
        assertEquals(QorConfig.EmailVerificationResendCooldownSeconds, vm.uiState.value.cooldown.remainingSeconds)
        assertFalse(vm.uiState.value.cooldown.canResend)
    }

    @Test
    fun `GIVEN the cooldown just started WHEN one second of virtual time elapses THEN the remaining count ticks down`() =
        runTest {
            val (vm, _) = viewModel()

            dispatcher.scheduler.advanceTimeBy(1_001)
            dispatcher.scheduler.runCurrent()

            assertEquals(QorConfig.EmailVerificationResendCooldownSeconds - 1, vm.uiState.value.cooldown.remainingSeconds)
        }
}
