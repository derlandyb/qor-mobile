package br.com.qualorock.androidApp.ui.viewmodel

import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import domain.event.MapBounds
import domain.event.usecase.GetMapEvents
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
import kotlin.test.assertTrue

/** In-file fake, same shape as `HomeFeedViewModelTest`'s — `androidApp` can't see `shared`'s `commonTest` fakes. */
private class FakeMapEventRepository(private val result: Result<List<Event>>) : EventRepository {
    var lastCity: City? = null
    var lastBounds: MapBounds? = null

    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage =
        error("not used by MapViewModelTest")

    override suspend fun findById(id: String): EventDetail = error("not used by MapViewModelTest")

    override suspend fun getMapEvents(city: City?, bounds: MapBounds?): List<Event> {
        lastCity = city
        lastBounds = bounds
        return result.getOrThrow()
    }
}

private fun sampleEvent(id: String, latitude: Double? = -20.3155, longitude: Double? = -40.3128) = Event(
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
    latitude = latitude,
    longitude = longitude,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

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
    fun `GIVEN a city query succeeds WHEN loadByCity is called THEN pins render for geocoded events`() = runTest {
        val repository = FakeMapEventRepository(Result.success(listOf(sampleEvent("e1"))))
        val viewModel = MapViewModel(GetMapEvents(repository))

        viewModel.loadByCity(City.Vitoria)
        dispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        check(state is MapUiState.Content)
        assertEquals(1, state.pins.size)
        assertEquals(City.Vitoria, repository.lastCity)
    }

    @Test
    fun `GIVEN a bounds query returns zero events WHEN loadByBounds is called THEN empty pins is valid Content, not Error`() = runTest {
        val repository = FakeMapEventRepository(Result.success(emptyList()))
        val viewModel = MapViewModel(GetMapEvents(repository))
        val bounds = MapBounds(north = 1.0, south = 0.0, east = 1.0, west = 0.0)

        viewModel.loadByBounds(bounds)
        dispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        check(state is MapUiState.Content)
        assertTrue(state.pins.isEmpty())
        assertEquals(bounds, repository.lastBounds)
    }

    @Test
    fun `GIVEN the map query fails WHEN loadByCity is called THEN Error is surfaced`() = runTest {
        val repository = FakeMapEventRepository(Result.failure(RuntimeException("boom")))
        val viewModel = MapViewModel(GetMapEvents(repository))

        viewModel.loadByCity(City.Vitoria)
        dispatcher.scheduler.runCurrent()

        assertEquals(MapUiState.Error, viewModel.uiState.value)
    }
}
