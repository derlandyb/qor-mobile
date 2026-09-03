package domain.event

import data.QorConfig
import domain.event.usecase.FakeEventRepository
import domain.event.usecase.ListUpcomingEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun sampleEvent(id: String) = Event(
    id = id,
    title = "Show",
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = domain.enum.City.Vitoria,
    genre = "Rock",
    address = "Rua X",
    isFree = true,
    ticketUrl = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PollingCoordinatorTest {

    @Test
    fun `GIVEN start is called WHEN the configured interval elapses THEN a new fetch feeds the events flow`() = runTest {
        val repository = FakeEventRepository(page = EventPage(listOf(sampleEvent("e1")), null))
        val coordinator = PollingCoordinator(ListUpcomingEvents(repository))

        coordinator.start(this)
        runCurrent()
        assertEquals(1, coordinator.events.value.events.size)

        advanceTimeBy(QorConfig.EventListPollIntervalSeconds * 1_000 + 1)
        runCurrent()

        assertEquals(1, coordinator.events.value.events.size)
        coordinator.stop()
    }

    @Test
    fun `GIVEN start has not yet ticked WHEN refreshNow is called THEN it fetches immediately without waiting for the interval`() = runTest {
        val repository = FakeEventRepository(page = EventPage(listOf(sampleEvent("e2")), null))
        val coordinator = PollingCoordinator(ListUpcomingEvents(repository))

        coordinator.refreshNow()

        assertEquals(1, coordinator.events.value.events.size)
        assertEquals("e2", coordinator.events.value.events.first().id)
    }
}
