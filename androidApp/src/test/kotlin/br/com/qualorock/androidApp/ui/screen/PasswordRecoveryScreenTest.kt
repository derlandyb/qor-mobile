package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import br.com.qualorock.androidApp.ui.viewmodel.PasswordRecoveryViewModel
import domain.enum.ConsentType
import domain.user.ConfirmResetResult
import domain.user.DataRightResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import domain.user.User
import domain.user.UserRepository
import domain.user.VerifyEmailResult
import domain.user.usecase.ResetPassword
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

/** In-file fake, same shape as `EmailVerificationScreenTest`'s — keeps this render test independent of Koin. */
private class FakePasswordRecoveryUserRepository(
    private val confirmResult: ConfirmResetResult,
) : UserRepository {
    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = error("not used by PasswordRecoveryScreenTest")

    override suspend fun login(email: String, password: String): LoginResult =
        error("not used by PasswordRecoveryScreenTest")

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult =
        error("not used by PasswordRecoveryScreenTest")

    override suspend fun logout() = Unit

    override suspend fun requestPasswordReset(email: String) = Unit

    override suspend fun confirmPasswordReset(token: String, newPassword: String): ConfirmResetResult = confirmResult

    override suspend fun resendVerification(email: String) = Unit

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult =
        error("not used by PasswordRecoveryScreenTest")

    override suspend fun getProfile(): User = error("not used by PasswordRecoveryScreenTest")
    override suspend fun updateProfile(fields: ProfileUpdateFields): User =
        error("not used by PasswordRecoveryScreenTest")

    override suspend fun accessData(): DataRightResult = error("not used by PasswordRecoveryScreenTest")
    override suspend fun exportData(): DataRightResult = error("not used by PasswordRecoveryScreenTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by PasswordRecoveryScreenTest")

    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        error("not used by PasswordRecoveryScreenTest")
}

private fun viewModel(confirmResult: ConfirmResetResult): PasswordRecoveryViewModel =
    PasswordRecoveryViewModel(ResetPassword(FakePasswordRecoveryUserRepository(confirmResult)))

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PasswordRecoveryScreenTest {

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
    fun `GIVEN it is rendered THEN the title and email step are shown`() {
        composeTestRule.setContent {
            PasswordRecoveryScreen(
                onResetSuccess = {},
                onNavigateToLogin = {},
                viewModel = viewModel(ConfirmResetResult.Failure("n/a")),
            )
        }

        composeTestRule.onNodeWithText("Recuperar senha").assertExists()
        composeTestRule.onNodeWithText("E-mail").assertExists()
        composeTestRule.onNodeWithText("Enviar link de recuperação").assertExists()
    }

    @Test
    fun `GIVEN an empty email WHEN submit is pressed THEN a required-email error is shown`() {
        composeTestRule.setContent {
            PasswordRecoveryScreen(
                onResetSuccess = {},
                onNavigateToLogin = {},
                viewModel = viewModel(ConfirmResetResult.Failure("n/a")),
            )
        }

        composeTestRule.onNodeWithText("Enviar link de recuperação").performClick()

        composeTestRule.onNodeWithText("Informe seu e-mail.").assertExists()
    }

    @Test
    fun `GIVEN a valid email WHEN submit is pressed THEN the generic confirmation and step 2 fields are shown`() {
        composeTestRule.setContent {
            PasswordRecoveryScreen(
                onResetSuccess = {},
                onNavigateToLogin = {},
                viewModel = viewModel(ConfirmResetResult.Failure("n/a")),
            )
        }

        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Enviar link de recuperação").performClick()

        composeTestRule.onNodeWithText(
            "Se este e-mail existir, você receberá um código de redefinição.",
        ).assertExists()
        composeTestRule.onNodeWithText("Código de verificação").assertExists()
        composeTestRule.onNodeWithText("Senha").assertExists()
        composeTestRule.onNodeWithText("Redefinir senha").assertExists()
    }

    @Test
    fun `GIVEN step 2 WHEN the use case fails THEN the server pt-BR message is shown inline`() {
        composeTestRule.setContent {
            PasswordRecoveryScreen(
                onResetSuccess = {},
                onNavigateToLogin = {},
                viewModel = viewModel(ConfirmResetResult.Failure("Link expirado ou já utilizado.")),
            )
        }

        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Enviar link de recuperação").performClick()
        composeTestRule.onNodeWithText("Código de verificação").performTextInput("123456")
        composeTestRule.onNodeWithText("Senha").performTextInput("supersenha123")
        composeTestRule.onNodeWithText("Redefinir senha").performClick()

        composeTestRule.onAllNodesWithText("Link expirado ou já utilizado.")[0].assertExists()
    }

    @Test
    fun `GIVEN step 2 WHEN the use case succeeds THEN onResetSuccess fires`() {
        var succeeded = false
        composeTestRule.setContent {
            PasswordRecoveryScreen(
                onResetSuccess = { succeeded = true },
                onNavigateToLogin = {},
                viewModel = viewModel(ConfirmResetResult.Success),
            )
        }

        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Enviar link de recuperação").performClick()
        composeTestRule.onNodeWithText("Código de verificação").performTextInput("123456")
        composeTestRule.onNodeWithText("Senha").performTextInput("supersenha123")
        composeTestRule.onNodeWithText("Redefinir senha").performClick()

        assert(succeeded)
    }

    @Test
    fun `GIVEN it is rendered THEN the back-to-login link fires onNavigateToLogin`() {
        var navigated = false
        composeTestRule.setContent {
            PasswordRecoveryScreen(
                onResetSuccess = {},
                onNavigateToLogin = { navigated = true },
                viewModel = viewModel(ConfirmResetResult.Failure("n/a")),
            )
        }

        composeTestRule.onNodeWithText("Lembrou da senha? Fazer login").performClick()

        assert(navigated)
    }
}
