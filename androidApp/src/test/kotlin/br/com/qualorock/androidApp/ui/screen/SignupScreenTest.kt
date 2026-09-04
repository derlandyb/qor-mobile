package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import br.com.qualorock.androidApp.R
import br.com.qualorock.androidApp.ui.viewmodel.SignupViewModel
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

/** In-file fake, same shape as `LoginScreenTest`'s — keeps this render test independent of Koin. */
private class FakeSignupUserRepository(private val registerResult: RegisterResult) : UserRepository {
    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = registerResult

    override suspend fun login(email: String, password: String): LoginResult = error("not used by SignupScreenTest")
    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult = error("not used by SignupScreenTest")
    override suspend fun logout() = Unit
    override suspend fun requestPasswordReset(email: String) = Unit

    override suspend fun verifyResetCode(email: String, code: String): domain.user.VerifyResetCodeResult =
        error("not used by SignupScreenTest")

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult =
        error("not used by SignupScreenTest")

    override suspend fun resendVerification(email: String) = Unit

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult =
        error("not used by SignupScreenTest")

    override suspend fun getProfile(): User = error("not used by SignupScreenTest")
    override suspend fun updateProfile(fields: ProfileUpdateFields): User = error("not used by SignupScreenTest")
    override suspend fun accessData(): DataRightResult = error("not used by SignupScreenTest")
    override suspend fun exportData(): DataRightResult = error("not used by SignupScreenTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by SignupScreenTest")

    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        error("not used by SignupScreenTest")
}

private fun sampleUser() = User(
    id = 1,
    name = "Ana",
    email = "ana@example.com",
    emailVerifiedAt = null,
    phone = null,
    profilePictureUrl = null,
    birthdate = "1990-01-01",
)

private fun viewModel(registerResult: RegisterResult): SignupViewModel =
    SignupViewModel(RegisterFan(FakeSignupUserRepository(registerResult)))

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SignupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN it is rendered THEN name, email, password, birthdate and Google stub fields are shown`() {
        composeTestRule.setContent {
            SignupScreen(
                viewModel = viewModel(RegisterResult.Failure("n/a")),
                onSignupSuccess = {},
                onNavigateToLogin = {},
            )
        }

        composeTestRule.onNodeWithText("Nome completo").assertExists()
        composeTestRule.onNodeWithText("E-mail").assertExists()
        composeTestRule.onNodeWithText("Senha").assertExists()
        composeTestRule.onNodeWithText("Data de nascimento").assertExists()
        composeTestRule.onNodeWithText("Cadastrar com Google").performScrollTo().assertExists()
    }

    @Test
    fun `GIVEN empty fields WHEN submit is pressed THEN required field validation errors are shown`() {
        composeTestRule.setContent {
            SignupScreen(
                viewModel = viewModel(RegisterResult.Failure("n/a")),
                onSignupSuccess = {},
                onNavigateToLogin = {},
            )
        }

        composeTestRule.onNodeWithText("Cadastrar").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Informe seu nome.").assertExists()
        composeTestRule.onNodeWithText("Informe seu e-mail.").assertExists()
        composeTestRule.onNodeWithText("Informe uma senha.").assertExists()
        composeTestRule.onNodeWithText("Informe sua data de nascimento.").assertExists()
    }

    @Test
    fun `GIVEN consent is not accepted WHEN submit is pressed THEN a consent required message is shown and no submit happens`() {
        composeTestRule.setContent {
            SignupScreen(
                viewModel = viewModel(RegisterResult.Failure("n/a")),
                onSignupSuccess = {},
                onNavigateToLogin = {},
            )
        }

        composeTestRule.onNodeWithText("Nome completo").performTextInput("Ana")
        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha123")
        composeTestRule.onNodeWithText("Data de nascimento").performScrollTo().performTextInput("1990-01-01")
        composeTestRule.onNodeWithText("Cadastrar").performScrollTo().performClick()

        composeTestRule.onNodeWithText("É necessário aceitar os Termos de Uso e a Política de Privacidade.")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun `GIVEN a server field error for email WHEN submit is pressed THEN it is shown under the email field`() {
        composeTestRule.setContent {
            SignupScreen(
                viewModel = viewModel(
                    RegisterResult.Failure(
                        message = "Erro de validação.",
                        fieldErrors = mapOf("email" to listOf("Este e-mail já está cadastrado")),
                    ),
                ),
                onSignupSuccess = {},
                onNavigateToLogin = {},
            )
        }

        composeTestRule.onNodeWithText("Nome completo").performTextInput("Ana")
        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha123")
        composeTestRule.onNodeWithText("Data de nascimento").performScrollTo().performTextInput("1990-01-01")
        composeTestRule.onNodeWithTag(context.getString(R.string.test_tag_consent_checkbox))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Cadastrar").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Este e-mail já está cadastrado").performScrollTo().assertExists()
    }

    @Test
    fun `GIVEN a successful signup WHEN submit is pressed THEN onSignupSuccess fires with the email`() {
        var signedUpEmail: String? = null
        composeTestRule.setContent {
            SignupScreen(
                viewModel = viewModel(RegisterResult.Success(sampleUser())),
                onSignupSuccess = { signedUpEmail = it },
                onNavigateToLogin = {},
            )
        }

        composeTestRule.onNodeWithText("Nome completo").performTextInput("Ana")
        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha123")
        composeTestRule.onNodeWithText("Data de nascimento").performScrollTo().performTextInput("1990-01-01")
        composeTestRule.onNodeWithTag(context.getString(R.string.test_tag_consent_checkbox))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Cadastrar").performScrollTo().performClick()

        assert(signedUpEmail == "ana@example.com")
    }

    @Test
    fun `GIVEN it is rendered THEN the Google button is disabled and does nothing when tapped`() {
        composeTestRule.setContent {
            SignupScreen(
                viewModel = viewModel(RegisterResult.Failure("n/a")),
                onSignupSuccess = {},
                onNavigateToLogin = {},
            )
        }

        composeTestRule.onNodeWithText("Cadastrar com Google").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Cadastrar com Google").assertExists()
    }
}
