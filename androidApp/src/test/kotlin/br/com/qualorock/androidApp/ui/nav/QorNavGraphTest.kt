package br.com.qualorock.androidApp.ui.nav

import android.app.Application
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import br.com.qualorock.androidApp.di.viewModelModule
import di.sharedModule
import data.SecureTokenStorage
import data.SessionStore
import domain.enum.City
import domain.enum.ConsentType
import domain.event.Event
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
 * Koin-independent fake ViewModel — this test starts Koin with [sharedModule]/[viewModelModule] plus fake
 * repository/token-storage overrides so no real network/Keystore calls happen.
 *
 * Also covers `.specs/tasks/mobile.md` A14's own "Done when" bar: an instrumented test for the
 * full login→home→detail→back path, and one deep-link case (`qualorock://evento/{id}`, see
 * [QorNavGraph]'s `eventDetailDestination`). Neither test uses an Espresso `Activity` host (this
 * module's other screen tests are bare [createComposeRule], not `createAndroidComposeRule`), so
 * "back" is exercised via the same [NavHostController.popBackStack] the system back button
 * triggers by default — no screen here installs a custom `BackHandler` to intercept it.
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

private class FakeUserRepository(
    private val profile: User?,
    private val loginResult: LoginResult = LoginResult.InvalidCredentials("not used in this test"),
) : UserRepository {
    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = RegisterResult.Failure("not used in this test")

    override suspend fun login(email: String, password: String): LoginResult = loginResult

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult =
        LoginResult.InvalidCredentials("not used in this test")

    override suspend fun logout() = Unit

    override suspend fun requestPasswordReset(email: String) = Unit

    override suspend fun verifyResetCode(email: String, code: String): domain.user.VerifyResetCodeResult =
        domain.user.VerifyResetCodeResult.Failure("not used in this test")

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult =
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

private class FakeEventRepository(
    private val events: List<Event> = emptyList(),
    private val detailById: Map<String, EventDetail> = emptyMap(),
) : EventRepository {
    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage =
        EventPage(events = events, nextCursor = null)

    override suspend fun findById(id: String): EventDetail =
        detailById[id] ?: error("no detail configured for event $id in this test")
}

private fun sampleEvent(id: String = "42", title: String = "Show de Rock") = Event(
    id = id,
    title = title,
    description = "Uma noite de rock no coração de Vitória.",
    coverImageUrl = null,
    startsAt = "2026-12-01T20:00:00Z",
    city = City.Vitoria,
    genre = "Rock",
    address = "Praça do Papa, Vitória",
    isFree = true,
    ticketUrl = null,
)

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class QorNavGraphTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun startTestKoin(
        userRepository: UserRepository,
        tokenStorage: SecureTokenStorage,
        eventRepository: EventRepository = FakeEventRepository(),
    ): SessionStore {
        val koinApp = startKoin {
            modules(
                sharedModule,
                viewModelModule,
                module {
                    single<UserRepository> { userRepository }
                    single<EventRepository> { eventRepository }
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

    @Test
    fun `GIVEN a fan WHEN they log in, open an event, and press back THEN they land back on Home`() {
        val user = User(
            id = "1",
            name = "Ana",
            email = "ana@example.com",
            emailVerifiedAt = "2026-01-01T00:00:00Z",
            phone = null,
            profilePictureUrl = null,
            birthdate = "1990-01-01",
        )
        val event = sampleEvent()
        val sessionStore = startTestKoin(
            userRepository = FakeUserRepository(
                profile = user,
                loginResult = LoginResult.Success(user = user, token = "a-valid-token"),
            ),
            tokenStorage = FakeTokenStorage(token = null),
            eventRepository = FakeEventRepository(events = listOf(event), detailById = mapOf(event.id to EventDetail.Active(event))),
        )
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            QorNavGraph(sessionStore = sessionStore, navController = navController)
        }
        composeTestRule.waitForIdle()

        // Login
        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("senha123")
        composeTestRule.onNodeWithText("Entrar").performClick()
        composeTestRule.waitForIdle()

        // Home
        composeTestRule.onNodeWithText("Início").assertExists()
        composeTestRule.onNodeWithText(event.title).performClick()
        composeTestRule.waitForIdle()

        // Detail
        composeTestRule.onNodeWithText(event.title).assertExists()

        // Back — same NavHost pop the system back button triggers by default (no BackHandler here).
        composeTestRule.runOnUiThread { navController.popBackStack() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Início").assertExists()
    }

    @Test
    fun `GIVEN a deep link to an event WHEN the app opens it THEN it renders that event's detail directly`() {
        val event = sampleEvent(id = "77", title = "Festival de Verão")
        val sessionStore = startTestKoin(
            userRepository = FakeUserRepository(profile = null),
            tokenStorage = FakeTokenStorage(token = null),
            eventRepository = FakeEventRepository(detailById = mapOf(event.id to EventDetail.Active(event))),
        )
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            QorNavGraph(sessionStore = sessionStore, navController = navController)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            navController.navigate(Uri.parse("qualorock://evento/${event.id}"))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(event.title).assertExists()
    }
}
