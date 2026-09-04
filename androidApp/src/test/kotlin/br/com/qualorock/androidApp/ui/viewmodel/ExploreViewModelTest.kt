package br.com.qualorock.androidApp.ui.viewmodel

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
import kotlin.test.assertNull

/**
 * In-file [EventRepository] fake — same shape/queueing convention as `HomeFeedViewModelTest`'s
 * (`androidApp` can't see `shared`'s `commonTest` `FakeEventRepository`). Also tracks every
 * `(city, genre, cursor)` triple requested, so DISC-14–DISC-18 tests can assert what was actually
 * sent to [EventRepository.findUpcoming] rather than just what came back.
 */
private class FakeExploreEventRepository(
    private val responses: MutableList<Result<EventPage>>,
) : EventRepository {
    val requestsReceived = mutableListOf<Triple<City?, String?, String?>>()

    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage {
        requestsReceived.add(Triple(city, genre, cursor))
        val next = if (responses.size > 1) responses.removeAt(0) else responses.first()
        return next.getOrThrow()
    }

    override suspend fun findById(id: String): EventDetail = error("not used by ExploreViewModelTest")
}

private fun sampleEvent(id: String, city: City = City.Vitoria, genre: String = "Rock") = Event(
    id = id,
    title = "Show $id",
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = city,
    genre = genre,
    address = "Rua X, 100",
    isFree = true,
    ticketUrl = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** See `HomeFeedViewModelTest`'s identical field for why cleanup must happen inside each test body. */
    private val createdViewModels = mutableListOf<ExploreViewModel>()

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
    ): Pair<ExploreViewModel, FakeExploreEventRepository> {
        val repository = FakeExploreEventRepository(responses.toMutableList())
        val listUpcomingEvents = ListUpcomingEvents(repository)
        val coordinator = PollingCoordinator(listUpcomingEvents)
        val vm = ExploreViewModel(listUpcomingEvents, coordinator)
        createdViewModels.add(vm)
        return vm to repository
    }

    @Test
    fun `GIVEN the default feed WHEN a city is selected THEN subsequent fetches carry that city`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val filtered = EventPage(listOf(sampleEvent("e2", city = City.Serra)), nextCursor = null)
        val (vm, repository) = viewModel(listOf(Result.success(settled), Result.success(settled), Result.success(filtered)))
        dispatcher.scheduler.runCurrent()

        vm.onCitySelected(City.Serra)
        dispatcher.scheduler.runCurrent()

        assertEquals(City.Serra, vm.selectedCity.value)
        assertEquals(City.Serra, repository.requestsReceived.last().first)
        val state = vm.uiState.value
        check(state is HomeFeedUiState.Content)
        assertEquals(listOf("e2"), state.events.map { it.id })
        vm.onCleared()
    }

    @Test
    fun `GIVEN the default feed WHEN a genre is selected THEN subsequent fetches carry that genre`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val filtered = EventPage(listOf(sampleEvent("e2", genre = "Samba")), nextCursor = null)
        val (vm, repository) = viewModel(listOf(Result.success(settled), Result.success(settled), Result.success(filtered)))
        dispatcher.scheduler.runCurrent()

        vm.onGenreSelected("Samba")
        dispatcher.scheduler.runCurrent()

        assertEquals("Samba", vm.selectedGenre.value)
        assertEquals("Samba", repository.requestsReceived.last().second)
        vm.onCleared()
    }

    @Test
    fun `GIVEN a city is selected WHEN a genre is also selected THEN both filters are AND-combined`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val filtered = EventPage(listOf(sampleEvent("e2")), nextCursor = null)
        val (vm, repository) = viewModel(
            listOf(Result.success(settled), Result.success(settled), Result.success(filtered), Result.success(filtered)),
        )
        dispatcher.scheduler.runCurrent()

        vm.onCitySelected(City.Serra)
        dispatcher.scheduler.runCurrent()
        vm.onGenreSelected("Samba")
        dispatcher.scheduler.runCurrent()

        val (city, genre, _) = repository.requestsReceived.last()
        assertEquals(City.Serra, city)
        assertEquals("Samba", genre)
        vm.onCleared()
    }

    @Test
    fun `GIVEN a city already selected WHEN it is selected again THEN it is cleared`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val (vm, _) = viewModel(listOf(Result.success(settled)))
        dispatcher.scheduler.runCurrent()
        vm.onCitySelected(City.Serra)
        dispatcher.scheduler.runCurrent()

        vm.onCitySelected(City.Serra)
        dispatcher.scheduler.runCurrent()

        assertNull(vm.selectedCity.value)
        vm.onCleared()
    }

    @Test
    fun `GIVEN filters are active WHEN onClearFilters is called THEN the default unfiltered list returns`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val (vm, repository) = viewModel(listOf(Result.success(settled)))
        dispatcher.scheduler.runCurrent()
        vm.onCitySelected(City.Serra)
        dispatcher.scheduler.runCurrent()
        vm.onGenreSelected("Samba")
        dispatcher.scheduler.runCurrent()

        vm.onClearFilters()
        dispatcher.scheduler.runCurrent()

        assertNull(vm.selectedCity.value)
        assertNull(vm.selectedGenre.value)
        val (city, genre, _) = repository.requestsReceived.last()
        assertNull(city)
        assertNull(genre)
        vm.onCleared()
    }

    @Test
    fun `GIVEN a filter is active WHEN onLoadMore is called THEN the next page request carries the same filter`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = "cursor-2")
        val filteredPage1 = EventPage(listOf(sampleEvent("e2")), nextCursor = "cursor-2")
        val filteredPage2 = EventPage(listOf(sampleEvent("e3")), nextCursor = null)
        val (vm, repository) = viewModel(
            listOf(
                Result.success(settled),
                Result.success(settled),
                Result.success(filteredPage1),
                Result.success(filteredPage1),
                Result.success(filteredPage2),
            ),
        )
        dispatcher.scheduler.runCurrent()
        vm.onCitySelected(City.Serra)
        dispatcher.scheduler.runCurrent()

        vm.onLoadMore()
        dispatcher.scheduler.runCurrent()

        val last = repository.requestsReceived.last()
        assertEquals(City.Serra, last.first)
        assertEquals("cursor-2", last.third)
        val state = vm.uiState.value
        check(state is HomeFeedUiState.Content)
        assertEquals(listOf("e2", "e3"), state.events.map { it.id })
        vm.onCleared()
    }

    @Test
    fun `GIVEN a filter is active WHEN the poll interval elapses THEN the filtered list still refreshes`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val filteredSettled = EventPage(listOf(sampleEvent("e2")), nextCursor = null)
        val tick = EventPage(listOf(sampleEvent("e2"), sampleEvent("e3")), nextCursor = null)
        val (vm, _) = viewModel(
            listOf(
                Result.success(settled),
                Result.success(settled),
                Result.success(filteredSettled),
                Result.success(filteredSettled),
                Result.success(tick),
            ),
        )
        dispatcher.scheduler.runCurrent()
        vm.onCitySelected(City.Serra)
        dispatcher.scheduler.runCurrent()

        dispatcher.scheduler.advanceTimeBy(data.QorConfig.EventListPollIntervalSeconds * 1_000 + 1)
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        check(state is HomeFeedUiState.Content)
        assertEquals(listOf("e2", "e3"), state.events.map { it.id })
        vm.onCleared()
    }

    @Test
    fun `GIVEN a filter yields no matches THEN the empty state is shown`() = runTest {
        val settled = EventPage(listOf(sampleEvent("e1")), nextCursor = null)
        val empty = EventPage(emptyList(), nextCursor = null)
        val (vm, _) = viewModel(listOf(Result.success(settled), Result.success(settled), Result.success(empty)))
        dispatcher.scheduler.runCurrent()

        vm.onCitySelected(City.Cariacica)
        dispatcher.scheduler.runCurrent()

        assertEquals(HomeFeedUiState.Empty, vm.uiState.value)
        vm.onCleared()
    }
}
