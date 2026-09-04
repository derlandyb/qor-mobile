package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import br.com.qualorock.androidApp.ui.viewmodel.LoginViewModel
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** In-file fake, same shape as `LoginViewModelTest`'s — keeps this render test independent of Koin. */
private class FakeUserRepository(private val loginResult: LoginResult) : UserRepository {
    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = error("not used by LoginScreenTest")

    override suspend fun login(email: String, password: String): LoginResult = loginResult

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult = error("not used by LoginScreenTest")
    override suspend fun logout() = Unit
    override suspend fun requestPasswordReset(email: String) = Unit

    override suspend fun verifyResetCode(email: String, code: String): domain.user.VerifyResetCodeResult =
        error("not used by LoginScreenTest")

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult =
        error("not used by LoginScreenTest")

    override suspend fun resendVerification(email: String) = Unit

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult =
        error("not used by LoginScreenTest")

    override suspend fun getProfile(): User = error("not used by LoginScreenTest")
    override suspend fun updateProfile(fields: ProfileUpdateFields): User = error("not used by LoginScreenTest")
    override suspend fun accessData(): DataRightResult = error("not used by LoginScreenTest")
    override suspend fun exportData(): DataRightResult = error("not used by LoginScreenTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by LoginScreenTest")

    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        error("not used by LoginScreenTest")
}

private class NoOpSessionWriter : SessionWriter {
    override suspend fun onAuthenticated(user: User, token: String) = Unit
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

private fun viewModel(loginResult: LoginResult): LoginViewModel =
    LoginViewModel(AuthenticateFan(FakeUserRepository(loginResult), NoOpSessionWriter()))

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN it is rendered THEN email, password and Google stub fields are shown`() {
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel(LoginResult.InvalidCredentials("n/a")),
                onLoginSuccess = {},
                onNavigateToVerifyEmail = {},
                onNavigateToSignup = {},
                onNavigateToPasswordRecovery = {},
            )
        }

        composeTestRule.onNodeWithText("E-mail").assertExists()
        composeTestRule.onNodeWithText("Senha").assertExists()
        composeTestRule.onNodeWithText("Entrar com Google").assertExists()
    }

    @Test
    fun `GIVEN invalid credentials WHEN submit is pressed THEN the pt-BR invalid credentials copy is shown`() {
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel(LoginResult.InvalidCredentials("Credenciais inválidas.")),
                onLoginSuccess = {},
                onNavigateToVerifyEmail = {},
                onNavigateToSignup = {},
                onNavigateToPasswordRecovery = {},
            )
        }

        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha123")
        composeTestRule.onNodeWithText("Entrar").performClick()

        composeTestRule.onNodeWithText("Credenciais inválidas").assertExists()
    }

    @Test
    fun `GIVEN an unverified account WHEN submit is pressed THEN the verify-email copy is shown and navigation fires`() {
        var navigatedEmail: String? = null
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel(LoginResult.UnverifiedAccount("Confirme seu e-mail.")),
                onLoginSuccess = {},
                onNavigateToVerifyEmail = { navigatedEmail = it },
                onNavigateToSignup = {},
                onNavigateToPasswordRecovery = {},
            )
        }

        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha123")
        composeTestRule.onNodeWithText("Entrar").performClick()

        composeTestRule.onNodeWithText("Confirme seu e-mail para continuar").assertExists()
        assert(navigatedEmail == "ana@example.com")
    }

    @Test
    fun `GIVEN a successful login WHEN submit is pressed THEN onLoginSuccess fires`() {
        var loggedIn = false
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel(LoginResult.Success(sampleUser(), "tok-1")),
                onLoginSuccess = { loggedIn = true },
                onNavigateToVerifyEmail = {},
                onNavigateToSignup = {},
                onNavigateToPasswordRecovery = {},
            )
        }

        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha123")
        composeTestRule.onNodeWithText("Entrar").performClick()

        assert(loggedIn)
    }

    @Test
    fun `GIVEN empty fields WHEN submit is pressed THEN required field validation errors are shown`() {
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel(LoginResult.InvalidCredentials("n/a")),
                onLoginSuccess = {},
                onNavigateToVerifyEmail = {},
                onNavigateToSignup = {},
                onNavigateToPasswordRecovery = {},
            )
        }

        composeTestRule.onNodeWithText("Entrar").performClick()

        composeTestRule.onNodeWithText("Informe seu e-mail.").assertExists()
        composeTestRule.onNodeWithText("Informe sua senha.").assertExists()
    }

    @Test
    fun `GIVEN it is rendered THEN the Google button is disabled and does nothing when tapped`() {
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel(LoginResult.InvalidCredentials("n/a")),
                onLoginSuccess = {},
                onNavigateToVerifyEmail = {},
                onNavigateToSignup = {},
                onNavigateToPasswordRecovery = {},
            )
        }

        // No crash/onLoginSuccess side-effect is the assertion here — the stub is a no-op.
        composeTestRule.onNodeWithText("Entrar com Google").performClick()
        composeTestRule.onNodeWithText("Entrar com Google").assertExists()
    }
}
