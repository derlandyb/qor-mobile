package com.qualorock.shared.feed

import com.qualorock.shared.data.EventPage
import com.qualorock.shared.data.EventRepository
import com.qualorock.shared.domain.Event
import com.qualorock.shared.domain.EventStatus
import com.qualorock.shared.domain.Venue
import com.qualorock.shared.domain.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeEventRepository(private val page: EventPage) : EventRepository {
    override suspend fun getEventFeed(
        cursor: String?,
        limit: Int,
    ): Result<EventPage> = Result.success(page)

    override suspend fun getEventDetail(id: String): Result<Event> = Result.failure(UnsupportedOperationException())
}

private class FailingEventRepository : EventRepository {
    override suspend fun getEventFeed(
        cursor: String?,
        limit: Int,
    ): Result<EventPage> = Result.failure(RuntimeException("network unavailable"))

    override suspend fun getEventDetail(id: String): Result<Event> = Result.failure(UnsupportedOperationException())
}

private fun event(
    id: String,
    startDateTime: Instant,
    status: EventStatus = EventStatus.PUBLISHED,
) = Event(
    id = id,
    title = "Show $id",
    coverImageUrl = null,
    startDateTime = startDateTime,
    venue =
        Venue(
            id = "venue-$id",
            name = "Venue $id",
            city = "Vitória",
            verificationStatus = VerificationStatus.VERIFIED,
        ),
    city = "Vitória",
    status = status,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EventFeedViewModelTest {
    @Test
    fun `given an anonymous visitor when the feed loads then upcoming events are grouped by date`() =
        runTest {
            val today = Instant.parse("2026-08-16T10:00:00Z")
            val tomorrow = Instant.parse("2026-08-17T20:00:00Z")
            val repository =
                FakeEventRepository(
                    EventPage(
                        events = listOf(event("1", today), event("2", tomorrow)),
                        nextCursor = null,
                    ),
                )
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)

            val viewModel = EventFeedViewModel(repository, scope)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(2, state.groupedEvents.size)
            assertEquals(listOf("1"), state.groupedEvents[0].events.map { it.id })
            assertEquals(listOf("2"), state.groupedEvents[1].events.map { it.id })
            assertTrue(state.endReached)
            assertEquals(null, state.error)
        }

    @Test
    fun `given the initial page fails then the retry state is reachable`() =
        runTest {
            val repository = FailingEventRepository()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)

            val viewModel = EventFeedViewModel(repository, scope)
            advanceUntilIdle()

            assertEquals(FeedError.INITIAL_LOAD, viewModel.state.value.error)
            assertTrue(viewModel.state.value.groupedEvents.isEmpty())
        }

    @Test
    fun `given zero events then the empty state is reachable`() =
        runTest {
            val repository = FakeEventRepository(EventPage(events = emptyList(), nextCursor = null))
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)

            val viewModel = EventFeedViewModel(repository, scope)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isEmpty)
        }
}
