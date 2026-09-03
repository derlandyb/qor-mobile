package domain.event.usecase

import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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

class GetEventDetailsTest {

    @Test
    fun `GIVEN an event id WHEN execute is called THEN it passes through unchanged to the repository`() = runTest {
        val repository = FakeEventRepository(detail = EventDetail.Active(sampleEvent("e1")))
        val useCase = GetEventDetails(repository)

        useCase.execute("e1")

        assertEquals("e1", repository.lastEventId)
    }

    @Test
    fun `GIVEN a cancelled event WHEN execute is called THEN the Cancelled variant propagates unchanged`() = runTest {
        val repository = FakeEventRepository(detail = EventDetail.Cancelled(sampleEvent("e2")))
        val useCase = GetEventDetails(repository)

        val result = useCase.execute("e2")

        assertIs<EventDetail.Cancelled>(result)
    }

    @Test
    fun `GIVEN an ended event WHEN execute is called THEN the Ended variant propagates unchanged`() = runTest {
        val repository = FakeEventRepository(detail = EventDetail.Ended(sampleEvent("e3")))
        val useCase = GetEventDetails(repository)

        val result = useCase.execute("e3")

        assertIs<EventDetail.Ended>(result)
    }
}
