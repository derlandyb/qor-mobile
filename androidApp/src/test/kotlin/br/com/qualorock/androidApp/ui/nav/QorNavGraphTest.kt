package br.com.qualorock.androidApp.ui.nav

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.com.qualorock.androidApp.di.appModule
import data.SecureTokenStorage
import data.SessionStore
import domain.enum.ConsentType
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import domain.user.ConfirmResetResult
import domain.user.DataRightResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import domain.user.User
import domain.user.UserRepository
import domain.user.VerifyEmailResult
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A14 — verifies [QorNavGraph]'s startup-restore decision (AUTH-12): an unauthenticated
 * [SessionStore] lands the fan on Login, a restorable session lands them on the authenticated
 * Home tab behind [br.com.qualorock.androidApp.ui.components.BottomNav]. Unlike per-screen tests
 * (e.g. `LoginScreenTest`), this graph is only reachable through production screens' default
 * `koinViewModel()`, so it needs a real (test-scoped) Koin container rather than a
 * Koin-independent fake ViewModel — this test starts Koin with [appModule] plus fake
 * repository/token-storage overrides so no real network/Keystore calls happen.
 */
private class FakeTokenStorage(private var token: String?) : SecureTokenStorage {
    override suspend fun save(token: String) {
        this.token = token
    }

    override suspend fun read(): String? = token

    override suspend fun clear() {
        token = null
    }
}

private class FakeUserRepository(private val profile: User?) : UserRepository {
    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = RegisterResult.Failure("not used in this test")

    override suspend fun login(email: String, password: String): LoginResult =
        LoginResult.InvalidCredentials("not used in this test")

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult =
        LoginResult.InvalidCredentials("not used in this test")

    override suspend fun logout() = Unit

    override suspend fun requestPasswordReset(email: String) = Unit

    override suspend fun confirmPasswordReset(token: String, newPassword: String): ConfirmResetResult =
        ConfirmResetResult.Failure("not used in this test")

    override suspend fun resendVerification(email: String) = Unit

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult =
        VerifyEmailResult.Failure("not used in this test")

    override suspend fun getProfile(): User = profile ?: error("no profile configured for this test")

    override suspend fun updateProfile(fields: ProfileUpdateFields): User =
        profile ?: error("no profile configured for this test")

    override suspend fun accessData(): DataRightResult = DataRightResult.Failure("not used in this test")
    override suspend fun exportData(): DataRightResult = DataRightResult.Failure("not used in this test")
    override suspend fun deleteAccount(): DataRightResult = DataRightResult.Failure("not used in this test")
    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        DataRightResult.Failure("not used in this test")
}

private class FakeEventRepository : EventRepository {
    override suspend fun findUpcoming(city: domain.enum.City?, genre: String?, cursor: String?): EventPage =
        EventPage(events = emptyList(), nextCursor = null)

    override suspend fun findById(id: String): EventDetail = error("not used in this test")
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class QorNavGraphTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun startTestKoin(userRepository: UserRepository, tokenStorage: SecureTokenStorage): SessionStore {
        val koinApp = startKoin {
            modules(
                appModule,
                module {
                    single<UserRepository> { userRepository }
                    single<EventRepository> { FakeEventRepository() }
                    single<SecureTokenStorage> { tokenStorage }
                },
            )
        }
        return koinApp.koin.get()
    }

    @Test
    fun `GIVEN no stored session WHEN the graph starts THEN it lands on Login`() {
        val sessionStore = startTestKoin(userRepository = FakeUserRepository(profile = null), tokenStorage = FakeTokenStorage(token = null))

        composeTestRule.setContent { QorNavGraph(sessionStore = sessionStore) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bem-vindo de volta").assertExists()
    }

    @Test
    fun `GIVEN a restorable session WHEN the graph starts THEN it lands on the authenticated Home tab`() {
        val user = User(
            id = "1",
            name = "Ana",
            email = "ana@example.com",
            emailVerifiedAt = "2026-01-01T00:00:00Z",
            phone = null,
            profilePictureUrl = null,
            birthdate = "1990-01-01",
        )
        val sessionStore = startTestKoin(
            userRepository = FakeUserRepository(profile = user),
            tokenStorage = FakeTokenStorage(token = "a-valid-token"),
        )

        composeTestRule.setContent { QorNavGraph(sessionStore = sessionStore) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Início").assertExists()
    }
}
