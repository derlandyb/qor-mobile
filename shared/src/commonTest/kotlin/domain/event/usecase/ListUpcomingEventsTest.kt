package domain.event.usecase

import domain.enum.City
import domain.event.Event
import domain.event.EventPage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun sampleEvent(id: String) = Event(
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
)

class ListUpcomingEventsTest {

    @Test
    fun `GIVEN city and genre filters WHEN execute is called THEN they pass through unchanged to the repository`() = runTest {
        val repository = FakeEventRepository(page = EventPage(listOf(sampleEvent("e1")), null))
        val useCase = ListUpcomingEvents(repository)

        useCase.execute(city = City.Serra, genre = "Samba", cursor = "cur-1")

        assertEquals(City.Serra, repository.lastCity)
        assertEquals("Samba", repository.lastGenre)
        assertEquals("cur-1", repository.lastCursor)
    }

    @Test
    fun `GIVEN an empty result WHEN execute is called THEN it returns an empty EventPage not an error`() = runTest {
        val repository = FakeEventRepository(page = EventPage(emptyList(), null))
        val useCase = ListUpcomingEvents(repository)

        val result = useCase.execute()

        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `GIVEN no filters WHEN execute is called THEN null filters pass through unchanged`() = runTest {
        val repository = FakeEventRepository(page = EventPage(emptyList(), null))
        val useCase = ListUpcomingEvents(repository)

        useCase.execute()

        assertEquals(null, repository.lastCity)
        assertEquals(null, repository.lastGenre)
        assertEquals(null, repository.lastCursor)
    }
}
