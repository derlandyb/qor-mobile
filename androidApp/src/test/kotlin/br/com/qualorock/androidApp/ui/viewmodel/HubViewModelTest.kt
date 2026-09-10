package br.com.qualorock.androidApp.ui.viewmodel

import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import domain.event.MapBounds
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

/** In-file fake, same shape as `HomeFeedViewModelTest`'s — `androidApp` can't see `shared`'s `commonTest` fakes. */
private class FakeHubEventRepository(private val result: Result<EventPage>) : EventRepository {
    var lastCity: City? = null

    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage {
        lastCity = city
        return result.getOrThrow()
    }

    override suspend fun findById(id: String): EventDetail = error("not used by HubViewModelTest")

    override suspend fun getMapEvents(city: City?, bounds: MapBounds?): List<Event> =
        error("not used by HubViewModelTest")
}

private fun sampleEvent(id: String, city: City) = Event(
    id = id,
    title = "Show $id",
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = city,
    genre = "Rock",
    address = "Rua X, 100",
    isFree = true,
    ticketUrl = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HubViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a city has published events WHEN load is called THEN only that city's events are shown`() = runTest {
        val repository = FakeHubEventRepository(Result.success(EventPage(listOf(sampleEvent("e1", City.Serra)), nextCursor = null)))
        val viewModel = HubViewModel(ListUpcomingEvents(repository))

        viewModel.load(City.Serra)
        dispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        check(state is HubUiState.Content)
        assertEquals(listOf("e1"), state.events.map { it.id })
        assertEquals(City.Serra, repository.lastCity)
    }

    @Test
    fun `GIVEN a city has zero published events WHEN load is called THEN Empty is shown, not an error`() = runTest {
        val repository = FakeHubEventRepository(Result.success(EventPage(emptyList(), nextCursor = null)))
        val viewModel = HubViewModel(ListUpcomingEvents(repository))

        viewModel.load(City.Cariacica)
        dispatcher.scheduler.runCurrent()

        assertEquals(HubUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `GIVEN the events query fails WHEN load is called THEN Error is surfaced`() = runTest {
        val repository = FakeHubEventRepository(Result.failure(RuntimeException("boom")))
        val viewModel = HubViewModel(ListUpcomingEvents(repository))

        viewModel.load(City.VilaVelha)
        dispatcher.scheduler.runCurrent()

        assertEquals(HubUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `GIVEN a Hub reached via a route param with no stored city preference WHEN load is called THEN it still renders that city`() = runTest {
        val repository = FakeHubEventRepository(Result.success(EventPage(listOf(sampleEvent("e9", City.Cariacica)), nextCursor = null)))
        val viewModel = HubViewModel(ListUpcomingEvents(repository))

        // No prior selection/session state exists anywhere on this ViewModel — `load` takes the
        // city as a direct call argument (the nav-graph route arg), never a stored preference.
        viewModel.load(City.Cariacica)
        dispatcher.scheduler.runCurrent()

        assertEquals(City.Cariacica, repository.lastCity)
        val state = viewModel.uiState.value
        check(state is HubUiState.Content)
        assertEquals(listOf("e9"), state.events.map { it.id })
    }
}
