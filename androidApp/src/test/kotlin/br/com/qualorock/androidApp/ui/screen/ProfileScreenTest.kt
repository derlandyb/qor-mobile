package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import br.com.qualorock.androidApp.ui.viewmodel.ProfileViewModel
import data.SecureTokenStorage
import data.SessionStore
import domain.enum.ConsentType
import domain.user.ConfirmResetResult
import domain.user.DataRightResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import domain.user.User
import domain.user.UserRepository
import domain.user.usecase.UpdateProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

private class FakeSecureTokenStorage : SecureTokenStorage {
    private var token: String? = null
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

/** In-file fake, same shape as other screens' render-test fakes — keeps this independent of Koin. */
private class FakeProfileUserRepository(private val updateResult: Result<User>) : UserRepository {
    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = error("not used by ProfileScreenTest")

    override suspend fun login(email: String, password: String): LoginResult = error("not used by ProfileScreenTest")
    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult = error("not used by ProfileScreenTest")
    override suspend fun logout() = Unit
    override suspend fun requestPasswordReset(email: String) = Unit
    override suspend fun verifyResetCode(email: String, code: String): domain.user.VerifyResetCodeResult =
        error("not used by ProfileScreenTest")

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult =
        error("not used by ProfileScreenTest")

    override suspend fun resendVerification(email: String) = Unit
    override suspend fun verifyEmailCode(email: String, code: String): domain.user.VerifyEmailResult =
        error("not used by ProfileScreenTest")

    override suspend fun getProfile(): User = error("not used by ProfileScreenTest")
    override suspend fun updateProfile(fields: ProfileUpdateFields): User = updateResult.getOrThrow()
    override suspend fun accessData(): DataRightResult = error("not used by ProfileScreenTest")
    override suspend fun exportData(): DataRightResult = error("not used by ProfileScreenTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by ProfileScreenTest")
    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult = error("not used by ProfileScreenTest")
}

private fun sampleUser() = User(
    id = "u1",
    name = "Ana",
    email = "ana@example.com",
    emailVerifiedAt = "2026-01-01T00:00:00Z",
    phone = "27999999999",
    profilePictureUrl = null,
    birthdate = "1990-01-01",
)

private fun viewModel(updateResult: Result<User> = Result.success(sampleUser())): ProfileViewModel {
    val repository = FakeProfileUserRepository(updateResult)
    val sessionStore = SessionStore(repository, FakeSecureTokenStorage())
    runBlocking { sessionStore.set(sampleUser(), "tok-1") }
    return ProfileViewModel(sessionStore, UpdateProfile(repository))
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ProfileScreenTest {

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
    fun `GIVEN a logged-in session WHEN rendered THEN the username, email, phone and birthdate are shown`() {
        composeTestRule.setContent {
            ProfileScreen(viewModel = viewModel(), onEmailChangePending = {})
        }

        composeTestRule.onNodeWithText("Ana").assertExists()
        composeTestRule.onNodeWithText("ana@example.com").assertExists()
        composeTestRule.onNodeWithText("27999999999").assertExists()
        composeTestRule.onNodeWithText("1990-01-01").assertExists()
    }

    @Test
    fun `GIVEN the name is edited WHEN saved successfully THEN the new name is reflected immediately`() {
        composeTestRule.setContent {
            ProfileScreen(viewModel = viewModel(Result.success(sampleUser().copy(name = "Ana Renomeada"))), onEmailChangePending = {})
        }

        composeTestRule.onNodeWithText("Nome completo").performTextClearance()
        composeTestRule.onNodeWithText("Nome completo").performTextInput("Ana Renomeada")
        composeTestRule.onAllNodesWithText("Salvar").onFirst().performClick()

        composeTestRule.onNodeWithText("Ana Renomeada").assertExists()
    }

    @Test
    fun `GIVEN a change picture button WHEN rendered THEN it is shown but disabled`() {
        composeTestRule.setContent {
            ProfileScreen(viewModel = viewModel(), onEmailChangePending = {})
        }

        composeTestRule.onNodeWithText("Alterar foto").assertExists()
        composeTestRule.onNodeWithText("Alterar foto").assertIsNotEnabled()
    }

    @Test
    fun `GIVEN the email is changed WHEN save succeeds THEN onEmailChangePending fires with the new email and the display stays unchanged`() {
        var pendingEmail: String? = null
        composeTestRule.setContent {
            ProfileScreen(viewModel = viewModel(), onEmailChangePending = { pendingEmail = it })
        }

        composeTestRule.onNodeWithText("E-mail").performTextClearance()
        composeTestRule.onNodeWithText("E-mail").performTextInput("nova@example.com")
        composeTestRule.onAllNodesWithText("Salvar").onLast().performClick()

        assert(pendingEmail == "nova@example.com")
        composeTestRule.onNodeWithText("ana@example.com").assertExists()
    }
}
