package br.com.qualorock.androidApp.ui.viewmodel

import domain.enum.ConsentType
import domain.user.ConfirmResetResult
import domain.user.DataRightResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import domain.user.User
import domain.user.UserRepository
import domain.user.VerifyEmailResult
import domain.user.VerifyResetCodeResult
import domain.user.usecase.ResetPassword
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
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Task-scoped in-file [UserRepository] fake, same shape as `EmailVerificationViewModelTest`'s —
 * keeps this test independent of Koin and of `shared`'s test-only fakes (not visible cross-module).
 */
private class FakePasswordRecoveryUserRepository(
    private val verifyResult: VerifyResetCodeResult,
    private val confirmResult: ConfirmResetResult,
) : UserRepository {
    var requestResetCallCount: Int = 0
    var lastRequestResetEmail: String? = null
    var lastVerifyEmail: String? = null
    var lastVerifyCode: String? = null
    var lastConfirmEmail: String? = null
    var lastConfirmToken: String? = null
    var lastConfirmPassword: String? = null

    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = error("not used by PasswordRecoveryViewModelTest")

    override suspend fun login(email: String, password: String): LoginResult =
        error("not used by PasswordRecoveryViewModelTest")

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult =
        error("not used by PasswordRecoveryViewModelTest")

    override suspend fun logout() = Unit

    override suspend fun requestPasswordReset(email: String) {
        requestResetCallCount += 1
        lastRequestResetEmail = email
    }

    override suspend fun verifyResetCode(email: String, code: String): VerifyResetCodeResult {
        lastVerifyEmail = email
        lastVerifyCode = code
        return verifyResult
    }

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult {
        lastConfirmEmail = email
        lastConfirmToken = token
        lastConfirmPassword = newPassword
        return confirmResult
    }

    override suspend fun resendVerification(email: String) = Unit

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult =
        error("not used by PasswordRecoveryViewModelTest")

    override suspend fun getProfile(): User = error("not used by PasswordRecoveryViewModelTest")

    override suspend fun updateProfile(fields: ProfileUpdateFields): User =
        error("not used by PasswordRecoveryViewModelTest")

    override suspend fun accessData(): DataRightResult = error("not used by PasswordRecoveryViewModelTest")
    override suspend fun exportData(): DataRightResult = error("not used by PasswordRecoveryViewModelTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by PasswordRecoveryViewModelTest")

    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        error("not used by PasswordRecoveryViewModelTest")
}

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordRecoveryViewModelTest {

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
        verifyResult: VerifyResetCodeResult = VerifyResetCodeResult.Failure("n/a"),
        confirmResult: ConfirmResetResult = ConfirmResetResult.Failure("n/a"),
    ): Pair<PasswordRecoveryViewModel, FakePasswordRecoveryUserRepository> {
        val repository = FakePasswordRecoveryUserRepository(verifyResult, confirmResult)
        return PasswordRecoveryViewModel(ResetPassword(repository)) to repository
    }

    @Test
    fun `GIVEN it is constructed THEN step 1 (request email) is shown`() = runTest {
        val (vm, _) = viewModel()

        assertIs<PasswordRecoveryStep.RequestEmail>(vm.uiState.value.step)
    }

    @Test
    fun `GIVEN an empty email WHEN onSubmitEmail is called THEN a required field error is set`() = runTest {
        val (vm, _) = viewModel()

        vm.onSubmitEmail()

        assertEquals(EmailFieldError.Required, vm.uiState.value.emailError)
        assertIs<PasswordRecoveryStep.RequestEmail>(vm.uiState.value.step)
    }

    @Test
    fun `GIVEN a malformed email WHEN onSubmitEmail is called THEN an invalid format error is set`() = runTest {
        val (vm, _) = viewModel()
        vm.onEmailChange("not-an-email")

        vm.onSubmitEmail()

        assertEquals(EmailFieldError.InvalidFormat, vm.uiState.value.emailError)
    }

    @Test
    fun `GIVEN a valid email WHEN onSubmitEmail is called THEN step 2 (verify code) advances regardless of backend response`() =
        runTest {
            val (vm, repository) = viewModel()
            vm.onEmailChange("ana@example.com")

            vm.onSubmitEmail()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, repository.requestResetCallCount)
            assertEquals("ana@example.com", repository.lastRequestResetEmail)
            val step = vm.uiState.value.step
            assertIs<PasswordRecoveryStep.VerifyCode>(step)
            assertEquals("ana@example.com", step.email)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `GIVEN step 2 WHEN onSubmitCode is called with an empty code THEN a required code error is set`() = runTest {
        val (vm, _) = viewModel()
        vm.onEmailChange("ana@example.com")
        vm.onSubmitEmail()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onSubmitCode()

        assertEquals(CodeFieldError.Required, vm.uiState.value.codeError)
    }

    @Test
    fun `GIVEN step 2 WHEN onSubmitCode is called with a short code THEN an invalid length error is set`() = runTest {
        val (vm, _) = viewModel()
        vm.onEmailChange("ana@example.com")
        vm.onSubmitEmail()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onCodeChange("123")

        vm.onSubmitCode()

        assertEquals(CodeFieldError.InvalidLength, vm.uiState.value.codeError)
    }

    @Test
    fun `GIVEN step 2 WHEN verifyResetCode returns Success THEN step 3 advances with the returned token`() = runTest {
        val (vm, repository) = viewModel(verifyResult = VerifyResetCodeResult.Success(token = "real-token-123"))
        vm.onEmailChange("ana@example.com")
        vm.onSubmitEmail()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onCodeChange("123456")

        vm.onSubmitCode()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("ana@example.com", repository.lastVerifyEmail)
        assertEquals("123456", repository.lastVerifyCode)
        val step = vm.uiState.value.step
        assertIs<PasswordRecoveryStep.NewPassword>(step)
        assertEquals("ana@example.com", step.email)
        assertEquals("real-token-123", step.token)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.submitError)
    }

    @Test
    fun `GIVEN step 2 WHEN verifyResetCode returns Failure THEN the error is shown inline and the fan stays on step 2`() =
        runTest {
            val (vm, _) = viewModel(verifyResult = VerifyResetCodeResult.Failure("Código inválido ou expirado."))
            vm.onEmailChange("ana@example.com")
            vm.onSubmitEmail()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onCodeChange("123456")

            vm.onSubmitCode()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals("Código inválido ou expirado.", vm.uiState.value.submitError)
            assertIs<PasswordRecoveryStep.VerifyCode>(vm.uiState.value.step)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `GIVEN step 2 failed once WHEN the fan retries with a valid code THEN step 3 advances`() = runTest {
        val repository = object : UserRepository by FakePasswordRecoveryUserRepository(
            verifyResult = VerifyResetCodeResult.Failure("n/a"),
            confirmResult = ConfirmResetResult.Success,
        ) {
            override suspend fun verifyResetCode(email: String, code: String): VerifyResetCodeResult =
                if (code == "123456") {
                    VerifyResetCodeResult.Success(token = "real-token-123")
                } else {
                    VerifyResetCodeResult.Failure("Código inválido ou expirado.")
                }
        }
        val vm = PasswordRecoveryViewModel(ResetPassword(repository))
        vm.onEmailChange("ana@example.com")
        vm.onSubmitEmail()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onCodeChange("000000")
        vm.onSubmitCode()
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<PasswordRecoveryStep.VerifyCode>(vm.uiState.value.step)

        vm.onCodeChange("123456")
        vm.onSubmitCode()
        dispatcher.scheduler.advanceUntilIdle()

        assertIs<PasswordRecoveryStep.NewPassword>(vm.uiState.value.step)
    }

    @Test
    fun `GIVEN step 3 WHEN onSubmitNewPassword is called with an empty password THEN a required password error is set`() =
        runTest {
            val (vm, _) = viewModel(verifyResult = VerifyResetCodeResult.Success(token = "tok"))
            vm.onEmailChange("ana@example.com")
            vm.onSubmitEmail()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onCodeChange("123456")
            vm.onSubmitCode()
            dispatcher.scheduler.advanceUntilIdle()

            vm.onSubmitNewPassword()

            assertEquals(NewPasswordFieldError.Required, vm.uiState.value.newPasswordError)
        }

    @Test
    fun `GIVEN step 3 WHEN onSubmitNewPassword is called with a too-short password THEN a too-short error is set`() =
        runTest {
            val (vm, _) = viewModel(verifyResult = VerifyResetCodeResult.Success(token = "tok"))
            vm.onEmailChange("ana@example.com")
            vm.onSubmitEmail()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onCodeChange("123456")
            vm.onSubmitCode()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onNewPasswordChange("short1")

            vm.onSubmitNewPassword()

            assertEquals(NewPasswordFieldError.TooShort, vm.uiState.value.newPasswordError)
        }

    @Test
    fun `GIVEN step 3 WHEN confirmReset returns Success THEN a ResetSuccess event is emitted using the step-2 token`() =
        runTest {
            val (vm, repository) = viewModel(
                verifyResult = VerifyResetCodeResult.Success(token = "real-token-123"),
                confirmResult = ConfirmResetResult.Success,
            )
            vm.onEmailChange("ana@example.com")
            vm.onSubmitEmail()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onCodeChange("123456")
            vm.onSubmitCode()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onNewPasswordChange("supersenha123")

            vm.onSubmitNewPassword()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(PasswordRecoveryEvent.ResetSuccess, vm.events.first())
            assertEquals("ana@example.com", repository.lastConfirmEmail)
            assertEquals("real-token-123", repository.lastConfirmToken)
            assertEquals("supersenha123", repository.lastConfirmPassword)
            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.submitError)
        }

    @Test
    fun `GIVEN step 3 WHEN confirmReset returns Failure THEN the server message is shown as an inline error`() =
        runTest {
            val (vm, _) = viewModel(
                verifyResult = VerifyResetCodeResult.Success(token = "real-token-123"),
                confirmResult = ConfirmResetResult.Failure("Link expirado ou já utilizado."),
            )
            vm.onEmailChange("ana@example.com")
            vm.onSubmitEmail()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onCodeChange("123456")
            vm.onSubmitCode()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onNewPasswordChange("supersenha123")

            vm.onSubmitNewPassword()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals("Link expirado ou já utilizado.", vm.uiState.value.submitError)
            assertFalse(vm.uiState.value.isLoading)
        }
}
