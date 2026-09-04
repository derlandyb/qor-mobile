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
import domain.user.usecase.RegisterFan
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
 * Minimal in-file [UserRepository] fake, same shape as `LoginViewModelTest`'s — keeps
 * [SignupViewModel] tests independent of a Koin test container and of `shared`'s test-only fakes.
 */
private class FakeSignupUserRepository(private val registerResult: RegisterResult) : UserRepository {
    var lastEmail: String? = null
    var lastPassword: String? = null
    var lastBirthdate: String? = null
    var lastName: String? = null
    var lastConsentAccepted: Boolean? = null

    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult {
        lastEmail = email
        lastPassword = password
        lastBirthdate = birthdate
        lastName = name
        lastConsentAccepted = consentAccepted
        return registerResult
    }

    override suspend fun login(email: String, password: String): LoginResult = error("not used by SignupViewModelTest")

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult = error("not used by SignupViewModelTest")

    override suspend fun logout() = Unit

    override suspend fun requestPasswordReset(email: String) = Unit

    override suspend fun verifyResetCode(email: String, code: String): domain.user.VerifyResetCodeResult =
        error("not used by SignupViewModelTest")

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult =
        error("not used by SignupViewModelTest")

    override suspend fun resendVerification(email: String) = Unit

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult =
        error("not used by SignupViewModelTest")

    override suspend fun getProfile(): User = error("not used by SignupViewModelTest")

    override suspend fun updateProfile(fields: ProfileUpdateFields): User = error("not used by SignupViewModelTest")

    override suspend fun accessData(): DataRightResult = error("not used by SignupViewModelTest")
    override suspend fun exportData(): DataRightResult = error("not used by SignupViewModelTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by SignupViewModelTest")

    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        error("not used by SignupViewModelTest")
}

private fun sampleUser() = User(
    id = "u1",
    name = "Ana",
    email = "ana@example.com",
    emailVerifiedAt = null,
    phone = null,
    profilePictureUrl = null,
    birthdate = "1990-01-01",
)

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(registerResult: RegisterResult): SignupViewModel =
        SignupViewModel(RegisterFan(FakeSignupUserRepository(registerResult)))

    @Test
    fun `GIVEN all fields are empty WHEN onSubmit is called THEN required field errors are set`() = runTest {
        val vm = viewModel(RegisterResult.Failure("n/a"))

        vm.onSubmit()

        assertEquals(NameFieldError.Required, vm.uiState.value.nameError)
        assertEquals(EmailFieldError.Required, vm.uiState.value.emailError)
        assertEquals(PasswordFieldError.Required, vm.uiState.value.passwordError)
        assertEquals(BirthdateFieldError.Required, vm.uiState.value.birthdateError)
    }

    @Test
    fun `GIVEN an invalid email format WHEN onSubmit is called THEN an email format error is set`() = runTest {
        val vm = viewModel(RegisterResult.Failure("n/a"))
        vm.onNameChange("Ana")
        vm.onEmailChange("not-an-email")
        vm.onPasswordChange("senha123")
        vm.onBirthdateChange("1990-01-01")
        vm.onConsentChange(true)

        vm.onSubmit()

        assertEquals(EmailFieldError.InvalidFormat, vm.uiState.value.emailError)
    }

    @Test
    fun `GIVEN consent is not accepted WHEN onSubmit is called THEN a consent error blocks submit`() = runTest {
        val vm = viewModel(RegisterResult.Failure("n/a"))
        vm.onNameChange("Ana")
        vm.onEmailChange("ana@example.com")
        vm.onPasswordChange("senha123")
        vm.onBirthdateChange("1990-01-01")

        vm.onSubmit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ConsentFieldError.Required, vm.uiState.value.consentError)
    }

    @Test
    fun `GIVEN consent is unchecked by default WHEN the view model is created THEN consentAccepted is false`() {
        val vm = viewModel(RegisterResult.Failure("n/a"))

        assertTrue(!vm.uiState.value.consentAccepted)
    }

    @Test
    fun `GIVEN a field was invalid WHEN it is edited again THEN its error is cleared`() = runTest {
        val vm = viewModel(RegisterResult.Failure("n/a"))
        vm.onSubmit()
        assertEquals(NameFieldError.Required, vm.uiState.value.nameError)

        vm.onNameChange("Ana")

        assertNull(vm.uiState.value.nameError)
    }

    @Test
    fun `GIVEN valid fields WHEN the repository returns a field error for email THEN it surfaces under the email field`() =
        runTest {
            val vm = viewModel(
                RegisterResult.Failure(
                    message = "Erro de validação.",
                    fieldErrors = mapOf("email" to listOf("Este e-mail já está cadastrado")),
                ),
            )
            vm.onNameChange("Ana")
            vm.onEmailChange("ana@example.com")
            vm.onPasswordChange("senha123")
            vm.onBirthdateChange("1990-01-01")
            vm.onConsentChange(true)

            vm.onSubmit()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals("Este e-mail já está cadastrado", vm.uiState.value.emailServerError)
            assertNull(vm.uiState.value.submitError)
            assertTrue(!vm.uiState.value.isLoading)
        }

    @Test
    fun `GIVEN valid fields WHEN the repository returns a failure with no known field errors THEN a generic error is set`() =
        runTest {
            val vm = viewModel(RegisterResult.Failure(message = "Algo deu errado."))
            vm.onNameChange("Ana")
            vm.onEmailChange("ana@example.com")
            vm.onPasswordChange("senha123")
            vm.onBirthdateChange("1990-01-01")
            vm.onConsentChange(true)

            vm.onSubmit()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals("Algo deu errado.", vm.uiState.value.submitError)
            assertNull(vm.uiState.value.emailServerError)
        }

    @Test
    fun `GIVEN valid fields WHEN the repository returns Success THEN a SignupSuccess event carries the email`() = runTest {
        val vm = viewModel(RegisterResult.Success(sampleUser()))
        vm.onNameChange("Ana")
        vm.onEmailChange("ana@example.com")
        vm.onPasswordChange("senha123")
        vm.onBirthdateChange("1990-01-01")
        vm.onConsentChange(true)

        vm.onSubmit()
        dispatcher.scheduler.advanceUntilIdle()

        val event = vm.events.first()
        assertEquals(SignupEvent.SignupSuccess("ana@example.com"), event)
        assertNull(vm.uiState.value.submitError)
    }
}
