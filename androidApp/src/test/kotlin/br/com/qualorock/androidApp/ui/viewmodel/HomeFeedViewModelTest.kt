package br.com.qualorock.androidApp.ui.viewmodel

import data.QorConfig
import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import domain.event.PollingCoordinator
import domain.event.usecase.ListUpcomingEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * In-file [EventRepository] fake, same shape as the auth screens' in-file `UserRepository`
 * fakes — `androidApp` only depends on `shared`'s main artifact, not `commonTest`, so
 * `shared`'s `FakeEventRepository` isn't visible here (same cross-module note prior A-task
 * tests leave).
 *
 * Queues a sequence of [Result]s, one per call, and repeats the last one forever once the queue
 * drains to a single entry — mirrors [PollingCoordinator]'s always-immediate first fetch: mounting
 * [HomeFeedViewModel] triggers *two* page-1 fetches back-to-back (its own direct initial load, then
 * [PollingCoordinator.start]'s own internal first tick — see [HomeFeedViewModel]'s KDoc), so every
 * test below queues that settle pair (equal values, so the second is a same-value no-op on the
 * [kotlinx.coroutines.flow.StateFlow] it feeds) before any response meant for an explicit action.
 */
private class FakeHomeFeedEventRepository(
    private val responses: MutableList<Result<EventPage>>,
) : EventRepository {
    var callCount: Int = 0
    val cursorsRequested = mutableListOf<String?>()

    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage {
        callCount += 1
        cursorsRequested.add(cursor)
        val next = if (responses.size > 1) responses.removeAt(0) else responses.first()
        return next.getOrThrow()
    }

    override suspend fun findById(id: String): EventDetail = error("not used by HomeFeedViewModelTest")
}

private fun sampleEvent(id: String) = Event(
    id = id,
    title = "Show $id",
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = City.Vitoria,
    genre = "Rock",
    address = "Rua X, 100",
    isFree = true,
    ticketUrl = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeFeedViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /**
     * [HomeFeedViewModel] always starts [PollingCoordinator]'s never-ending poll loop (see its
     * KDoc). `runTest`'s own wind-down calls the equivalent of `advanceUntilIdle()` against
     * `Dispatchers.Main` *before* this class's `@After` runs, so that loop must be stopped via
     * [HomeFeedViewModel.onCleared] from *inside* every test body (last thing each test does) —
     * cleaning up in `tearDown` alone is too late and hangs `runTest` forever draining an
     * infinitely-recurring `delay()`. [tearDown] still clears every tracked instance as a safety
     * net for a test that exits early (e.g. a failed assertion) before reaching its own cleanup.
     */
    private val createdViewModels = mutableListOf<HomeFeedViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        createdViewModels.forEach { it.onCleared() }
        Dispatchers.resetMain()
    }

    private fun viewModel(
        responses: List<Result<EventPage>>,
    ): Pair<HomeFeedViewModel, FakeHomeFeedEventRepository> {
        val repository = FakeHomeFeedEventRepository(responses.toMutableList())
        val listUpcomingEvents = ListUpcomingEvents(repository)
        val coordinator = PollingCoordinator(listUpcomingEvents)
        val vm = HomeFeedViewModel(listUpcomingEvents, coordinator)
        createdViewModels.add(vm)
        return vm to repository
    }

    @Test
    fun `GIVEN the screen is entered WHEN the initial load succeeds THEN the events list is populated`() = runTest {
        val page = EventPage(listOf(sampleEvent("e1"), sampleEvent("e2")), nextCursor = "cursor-2")
        val (vm, _) = viewModel(listOf(Result.success(page)))

        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        check(state is HomeFeedUiState.Content)
        assertEquals(listOf("e1", "e2"), state.events.map { it.id })
        assertFalse(state.isLoadingMore)
        vm.onCleared()
    }

    @Test
    fun `GIVEN a next cursor is available WHEN onLoadMore is called THEN the next page is fetched and appended`() =
        runTest {
            val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = "cursor-2")
            val nextPage = EventPage(listOf(sampleEvent("e2")), nextCursor = null)
            val (vm, repository) = viewModel(listOf(Result.success(settled), Result.success(settled), Result.success(nextPage)))
            dispatcher.scheduler.runCurrent()

            vm.onLoadMore()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value
            check(state is HomeFeedUiState.Content)
            assertEquals(listOf("e1", "e2"), state.events.map { it.id })
            assertFalse(state.isLoadingMore)
            assertEquals("cursor-2", repository.cursorsRequested.last())
            vm.onCleared()
        }

    @Test
    fun `GIVEN the repository has no more pages WHEN onLoadMore is called THEN it is a no-op`() = runTest {
        val page = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val (vm, repository) = viewModel(listOf(Result.success(page)))
        dispatcher.scheduler.runCurrent()
        val countAfterSettle = repository.callCount

        vm.onLoadMore()
        dispatcher.scheduler.runCurrent()

        assertEquals(countAfterSettle, repository.callCount)
        vm.onCleared()
    }

    @Test
    fun `GIVEN the initial load returns no events THEN the empty state is shown`() = runTest {
        val (vm, _) = viewModel(listOf(Result.success(EventPage(emptyList(), null))))

        dispatcher.scheduler.runCurrent()

        assertEquals(HomeFeedUiState.Empty, vm.uiState.value)
        vm.onCleared()
    }

    @Test
    fun `GIVEN the screen is entered WHEN the initial load fails THEN the error state is shown`() = runTest {
        val (vm, _) = viewModel(listOf(Result.failure(RuntimeException("boom"))))

        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        check(state is HomeFeedUiState.Error)
        vm.onCleared()
    }

    @Test
    fun `GIVEN the screen is entered WHEN the poll interval elapses THEN the events list refreshes`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val tick = EventPage(listOf(sampleEvent("e1"), sampleEvent("e2")), nextCursor = null)
        val (vm, _) = viewModel(listOf(Result.success(settled), Result.success(settled), Result.success(tick)))
        dispatcher.scheduler.runCurrent()

        dispatcher.scheduler.advanceTimeBy(QorConfig.EventListPollIntervalSeconds * 1_000 + 1)
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        check(state is HomeFeedUiState.Content)
        assertEquals(listOf("e1", "e2"), state.events.map { it.id })
        vm.onCleared()
    }

    @Test
    fun `GIVEN content is shown WHEN onRefresh is called THEN a manual fetch replaces the list`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val refreshed = EventPage(listOf(sampleEvent("e3")), nextCursor = null)
        val (vm, _) = viewModel(listOf(Result.success(settled), Result.success(settled), Result.success(refreshed)))
        dispatcher.scheduler.runCurrent()

        vm.onRefresh()
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        check(state is HomeFeedUiState.Content)
        assertEquals(listOf("e3"), state.events.map { it.id })
        assertFalse(state.isRefreshing)
        vm.onCleared()
    }

    @Test
    fun `GIVEN the ViewModel is cleared THEN polling stops`() = runTest {
        val page = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val (vm, repository) = viewModel(listOf(Result.success(page)))
        dispatcher.scheduler.runCurrent()
        val countAfterInitialLoad = repository.callCount

        vm.onCleared()
        dispatcher.scheduler.advanceTimeBy(QorConfig.EventListPollIntervalSeconds * 1_000 + 1)
        dispatcher.scheduler.runCurrent()

        assertEquals(countAfterInitialLoad, repository.callCount)
        assertTrue(countAfterInitialLoad >= 1)
    }
}
