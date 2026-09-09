package domain.event.usecase

import domain.enum.City
import domain.event.Event
import domain.event.MapBounds
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun sampleEvent(id: String, latitude: Double? = null, longitude: Double? = null) = Event(
    id = id,
    title = "Show",
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = City.Vitoria,
    genre = "Rock",
    address = "Rua X",
    isFree = true,
    ticketUrl = null,
    latitude = latitude,
    longitude = longitude,
)

class GetMapEventsTest {

    @Test
    fun `GIVEN a city WHEN execute is called THEN it passes through unchanged to the repository as a city-mode query`() = runTest {
        val repository = FakeEventRepository(mapEvents = listOf(sampleEvent("e1", -20.3, -40.3)))
        val useCase = GetMapEvents(repository)

        useCase.execute(city = City.Vitoria)

        assertEquals(City.Vitoria, repository.lastMapCity)
        assertEquals(null, repository.lastMapBounds)
    }

    @Test
    fun `GIVEN a bounding box WHEN execute is called THEN it passes through unchanged to the repository as a box-mode query`() = runTest {
        val repository = FakeEventRepository(mapEvents = listOf(sampleEvent("e1", -20.3, -40.3)))
        val useCase = GetMapEvents(repository)
        val bounds = MapBounds(north = -20.0, south = -20.5, east = -40.0, west = -40.5)

        useCase.execute(bounds = bounds)

        assertEquals(null, repository.lastMapCity)
        assertEquals(bounds, repository.lastMapBounds)
    }

    @Test
    fun `GIVEN a response with geocoded events WHEN execute is called THEN it maps to a successful Event list`() = runTest {
        val repository = FakeEventRepository(mapEvents = listOf(sampleEvent("e1", -20.3, -40.3)))
        val useCase = GetMapEvents(repository)

        val result = useCase.execute(city = City.Vitoria)

        assertEquals(Result.success(listOf(sampleEvent("e1", -20.3, -40.3))), result)
    }

    @Test
    fun `GIVEN an event with null coordinates in the response WHEN execute is called THEN mapping does not crash`() = runTest {
        val repository = FakeEventRepository(mapEvents = listOf(sampleEvent("e1", null, null)))
        val useCase = GetMapEvents(repository)

        val result = useCase.execute(city = City.Vitoria)

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrThrow().first().latitude)
    }

    @Test
    fun `GIVEN the repository throws WHEN execute is called THEN the error surfaces as a Result failure not an exception`() = runTest {
        val repository = ThrowingEventRepository()
        val useCase = GetMapEvents(repository)

        val result = useCase.execute(city = City.Vitoria)

        assertTrue(result.isFailure)
    }
}

private class ThrowingEventRepository : domain.event.EventRepository {
    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?) =
        error("not used by GetMapEventsTest")

    override suspend fun findById(id: String) = error("not used by GetMapEventsTest")

    override suspend fun getMapEvents(city: City?, bounds: MapBounds?): List<Event> =
        error("network error")
}
