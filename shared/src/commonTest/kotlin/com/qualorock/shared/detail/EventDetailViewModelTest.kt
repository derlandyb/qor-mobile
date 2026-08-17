package com.qualorock.shared.detail

import com.qualorock.shared.data.EventNotFoundException
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun event(id: String) =
    Event(
        id = id,
        title = "Show $id",
        startDateTime = Instant.parse("2026-08-16T22:00:00Z"),
        venue = Venue(id = "venue-$id", name = "Venue $id", city = "Vitória", verificationStatus = VerificationStatus.VERIFIED),
        city = "Vitória",
        status = EventStatus.PUBLISHED,
    )

private class FakeEventRepository(
    private val detailResult: Result<Event>,
) : EventRepository {
    var loadCount = 0

    override suspend fun getEventFeed(
        cursor: String?,
        limit: Int,
    ): Result<EventPage> = Result.success(EventPage(events = emptyList(), nextCursor = null))

    override suspend fun getEventDetail(id: String): Result<Event> {
        loadCount++
        return detailResult
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {
    @Test
    fun `given a successful fetch when load is called then the state becomes Loaded`() =
        runTest {
            val repository = FakeEventRepository(Result.success(event("1")))
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val viewModel = EventDetailViewModel(repository, "1", scope)

            viewModel.load()
            advanceUntilIdle()

            val state = assertIs<EventDetailUiState.Loaded>(viewModel.state.value)
            assertEquals("1", state.event.id)
        }

    @Test
    fun `given an EventNotFoundException when load is called then the state becomes NotFound`() =
        runTest {
            val repository = FakeEventRepository(Result.failure(EventNotFoundException("missing")))
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val viewModel = EventDetailViewModel(repository, "missing", scope)

            viewModel.load()
            advanceUntilIdle()

            assertIs<EventDetailUiState.NotFound>(viewModel.state.value)
        }

    @Test
    fun `given a generic failure when load is called then the state becomes LoadError`() =
        runTest {
            val repository = FakeEventRepository(Result.failure(RuntimeException("network unavailable")))
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val viewModel = EventDetailViewModel(repository, "1", scope)

            viewModel.load()
            advanceUntilIdle()

            assertIs<EventDetailUiState.LoadError>(viewModel.state.value)
        }

    @Test
    fun `given a LoadError state when retry is called then load is re-invoked`() =
        runTest {
            val repository = FakeEventRepository(Result.failure(RuntimeException("network unavailable")))
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val viewModel = EventDetailViewModel(repository, "1", scope)

            viewModel.load()
            advanceUntilIdle()
            viewModel.retry()
            advanceUntilIdle()

            assertTrue(repository.loadCount >= 2)
            assertIs<EventDetailUiState.LoadError>(viewModel.state.value)
        }
}
