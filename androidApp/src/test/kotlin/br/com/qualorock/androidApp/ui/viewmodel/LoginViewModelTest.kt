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
import domain.user.usecase.AuthenticateFan
import domain.user.usecase.SessionWriter
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Minimal in-file [UserRepository] fake so [LoginViewModel] tests exercise the real
 * [AuthenticateFan] use case without a Koin test container (none exists yet for ViewModels)
 * and without depending on `shared`'s test-only `FakeUserRepository` (not visible cross-module).
 */
private class FakeUserRepository(private val loginResult: LoginResult) : UserRepository {
    var lastEmail: String? = null
    var lastPassword: String? = null

    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = error("not used by LoginViewModelTest")

    override suspend fun login(email: String, password: String): LoginResult {
        lastEmail = email
        lastPassword = password
        return loginResult
    }

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult = error("not used by LoginViewModelTest")

    override suspend fun logout() = Unit

    override suspend fun requestPasswordReset(email: String) = Unit

    override suspend fun verifyResetCode(email: String, code: String): domain.user.VerifyResetCodeResult =
        error("not used by LoginViewModelTest")

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult =
        error("not used by LoginViewModelTest")

    override suspend fun resendVerification(email: String) = Unit

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult =
        error("not used by LoginViewModelTest")

    override suspend fun getProfile(): User = error("not used by LoginViewModelTest")

    override suspend fun updateProfile(fields: ProfileUpdateFields): User = error("not used by LoginViewModelTest")

    override suspend fun accessData(): DataRightResult = error("not used by LoginViewModelTest")
    override suspend fun exportData(): DataRightResult = error("not used by LoginViewModelTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by LoginViewModelTest")

    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        error("not used by LoginViewModelTest")
}

private class FakeSessionWriter : SessionWriter {
    var authenticatedUser: User? = null

    override suspend fun onAuthenticated(user: User, token: String) {
        authenticatedUser = user
    }
}

private fun sampleUser() = User(
    id = 1,
    name = "Ana",
    email = "ana@example.com",
    emailVerifiedAt = "2026-01-01T00:00:00Z",
    phone = null,
    profilePictureUrl = null,
    birthdate = "1990-01-01",
)

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(loginResult: LoginResult): LoginViewModel =
        LoginViewModel(AuthenticateFan(FakeUserRepository(loginResult), FakeSessionWriter()))

    @Test
    fun `GIVEN both fields are empty WHEN onSubmit is called THEN required field errors are set`() = runTest {
        val vm = viewModel(LoginResult.InvalidCredentials("n/a"))

        vm.onSubmit()

        assertEquals(EmailFieldError.Required, vm.uiState.value.emailError)
        assertEquals(PasswordFieldError.Required, vm.uiState.value.passwordError)
    }

    @Test
    fun `GIVEN an invalid email format WHEN onSubmit is called THEN an email format error is set`() = runTest {
        val vm = viewModel(LoginResult.InvalidCredentials("n/a"))
        vm.onEmailChange("not-an-email")
        vm.onPasswordChange("senha123")

        vm.onSubmit()

        assertEquals(EmailFieldError.InvalidFormat, vm.uiState.value.emailError)
        assertNull(vm.uiState.value.passwordError)
    }

    @Test
    fun `GIVEN a field was invalid WHEN it is edited again THEN its error is cleared`() = runTest {
        val vm = viewModel(LoginResult.InvalidCredentials("n/a"))
        vm.onSubmit()
        assertEquals(EmailFieldError.Required, vm.uiState.value.emailError)

        vm.onEmailChange("a")

        assertNull(vm.uiState.value.emailError)
    }

    @Test
    fun `GIVEN valid fields WHEN the repository returns InvalidCredentials THEN the submit error is set and loading ends`() =
        runTest {
            val vm = viewModel(LoginResult.InvalidCredentials("Credenciais inválidas."))
            vm.onEmailChange("ana@example.com")
            vm.onPasswordChange("senha123")

            vm.onSubmit()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(LoginSubmitError.InvalidCredentials, vm.uiState.value.submitError)
            assertTrue(!vm.uiState.value.isLoading)
        }

    @Test
    fun `GIVEN valid fields WHEN the repository returns UnverifiedAccount THEN a navigate event carries the email`() =
        runTest {
            val vm = viewModel(LoginResult.UnverifiedAccount("Confirme seu e-mail."))
            vm.onEmailChange("ana@example.com")
            vm.onPasswordChange("senha123")

            vm.onSubmit()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                LoginSubmitError.UnverifiedAccount("ana@example.com"),
                vm.uiState.value.submitError,
            )
            val event = vm.events.first()
            assertEquals(LoginEvent.NavigateToVerifyEmail("ana@example.com"), event)
        }

    @Test
    fun `GIVEN valid fields WHEN the repository returns Success THEN a LoginSuccess event is emitted`() = runTest {
        val vm = viewModel(LoginResult.Success(sampleUser(), "tok-1"))
        vm.onEmailChange("ana@example.com")
        vm.onPasswordChange("senha123")

        vm.onSubmit()
        dispatcher.scheduler.advanceUntilIdle()

        val event = vm.events.first()
        assertEquals(LoginEvent.LoginSuccess, event)
        assertNull(vm.uiState.value.submitError)
    }
}
