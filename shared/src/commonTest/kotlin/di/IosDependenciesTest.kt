package di

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * I7-I14 — confirms every [IosDependencies] accessor actually resolves against [sharedModule]'s
 * real Koin graph (the same one Android's `viewModelModule` builds on top of), rather than only
 * being compile-checked. A missing/mistyped binding here would otherwise only surface at runtime,
 * on-device, in the iOS app.
 */
class IosDependenciesTest {
    @BeforeTest
    fun setUp() {
        startKoin { modules(sharedModule) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun test_GIVEN_theSharedKoinGraphIsStarted_WHEN_resolvingEveryIosDependenciesAccessor_THEN_noneOfThemThrow() {
        IosDependencies.authenticateFan()
        IosDependencies.registerFan()
        IosDependencies.verifyEmail()
        IosDependencies.resetPassword()
        IosDependencies.updateProfile()
        IosDependencies.exerciseDataRight()
        IosDependencies.listUpcomingEvents()
        IosDependencies.getEventDetails()
        IosDependencies.pollingCoordinator()
        IosDependencies.sessionStore()
    }

    @Test
    fun test_GIVEN_pollingCoordinatorIsAKoinFactory_WHEN_resolvingItTwice_THEN_eachCallReturnsADistinctInstance() {
        val first = IosDependencies.pollingCoordinator()
        val second = IosDependencies.pollingCoordinator()

        // AD-021/STATE.md: Home and Explore must never share one PollingCoordinator instance.
        assertNotSame(first, second, "pollingCoordinator() must be a Koin factory, not a shared singleton")
    }

    @Test
    fun test_GIVEN_sessionStoreIsAKoinSingle_WHEN_resolvingItTwice_THEN_bothCallsReturnTheSameInstance() {
        val first = IosDependencies.sessionStore()
        val second = IosDependencies.sessionStore()

        assertSame(first, second, "sessionStore() must resolve the same shared session-state instance")
    }
}
