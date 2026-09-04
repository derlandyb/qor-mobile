package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import br.com.qualorock.androidApp.ui.viewmodel.EmailVerificationViewModel
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
private class FakeVerifyEmailUserRepository(private val verifyResult: VerifyEmailResult) : UserRepository {
    var resendCallCount: Int = 0

    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = error("not used by EmailVerificationScreenTest")

    override suspend fun login(email: String, password: String): LoginResult =
        error("not used by EmailVerificationScreenTest")

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult =
        error("not used by EmailVerificationScreenTest")

    override suspend fun logout() = Unit
    override suspend fun requestPasswordReset(email: String) = Unit

    override suspend fun verifyResetCode(email: String, code: String): domain.user.VerifyResetCodeResult =
        error("not used by EmailVerificationScreenTest")

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult =
        error("not used by EmailVerificationScreenTest")

    override suspend fun resendVerification(email: String) {
        resendCallCount += 1
    }

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult = verifyResult

    override suspend fun getProfile(): User = error("not used by EmailVerificationScreenTest")
    override suspend fun updateProfile(fields: ProfileUpdateFields): User =
        error("not used by EmailVerificationScreenTest")

    override suspend fun accessData(): DataRightResult = error("not used by EmailVerificationScreenTest")
    override suspend fun exportData(): DataRightResult = error("not used by EmailVerificationScreenTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by EmailVerificationScreenTest")

    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        error("not used by EmailVerificationScreenTest")
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

private fun viewModel(verifyResult: VerifyEmailResult): EmailVerificationViewModel =
    EmailVerificationViewModel(VerifyEmail(FakeVerifyEmailUserRepository(verifyResult)))

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class EmailVerificationScreenTest {

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
    fun `GIVEN it is rendered THEN the title, email and code field are shown`() {
        composeTestRule.setContent {
            EmailVerificationScreen(
                email = "ana@example.com",
                onVerified = {},
                viewModel = viewModel(VerifyEmailResult.Failure("n/a")),
            )
        }

        composeTestRule.onNodeWithText("Confirme seu e-mail").assertExists()
        composeTestRule.onNodeWithText("Código de verificação").assertExists()
        composeTestRule.onNodeWithText(
            "Enviamos um código de 6 dígitos para ana@example.com. Insira-o abaixo para confirmar sua conta.",
        ).assertExists()
    }

    @Test
    fun `GIVEN it is rendered THEN the resend action starts disabled with a countdown`() {
        composeTestRule.setContent {
            EmailVerificationScreen(
                email = "ana@example.com",
                onVerified = {},
                viewModel = viewModel(VerifyEmailResult.Failure("n/a")),
            )
        }

        composeTestRule.onNodeWithText("Reenviar em 60s").assertExists()
    }

    @Test
    fun `GIVEN an empty code WHEN submit is pressed THEN a required-code error is shown`() {
        composeTestRule.setContent {
            EmailVerificationScreen(
                email = "ana@example.com",
                onVerified = {},
                viewModel = viewModel(VerifyEmailResult.Failure("n/a")),
            )
        }

        composeTestRule.onNodeWithText("Verificar código").performClick()

        composeTestRule.onNodeWithText("Informe o código recebido.").assertExists()
    }

    @Test
    fun `GIVEN a valid code WHEN the use case fails THEN the server pt-BR message is shown inline`() {
        composeTestRule.setContent {
            EmailVerificationScreen(
                email = "ana@example.com",
                onVerified = {},
                viewModel = viewModel(VerifyEmailResult.Failure("Código inválido ou expirado.")),
            )
        }

        composeTestRule.onNodeWithText("Código de verificação").performTextInput("123456")
        composeTestRule.onNodeWithText("Verificar código").performClick()

        composeTestRule.onNodeWithText("Código inválido ou expirado.").assertExists()
    }

    @Test
    fun `GIVEN a valid code WHEN the use case succeeds THEN onVerified fires`() {
        var verified = false
        composeTestRule.setContent {
            EmailVerificationScreen(
                email = "ana@example.com",
                onVerified = { verified = true },
                viewModel = viewModel(VerifyEmailResult.Success(sampleUser())),
            )
        }

        composeTestRule.onNodeWithText("Código de verificação").performTextInput("123456")
        composeTestRule.onNodeWithText("Verificar código").performClick()

        assert(verified)
    }
}
