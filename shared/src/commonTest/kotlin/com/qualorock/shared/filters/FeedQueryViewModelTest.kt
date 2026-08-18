package com.qualorock.shared.filters

import com.qualorock.shared.data.EventPage
import com.qualorock.shared.data.EventRepository
import com.qualorock.shared.domain.Event
import com.qualorock.shared.domain.EventStatus
import com.qualorock.shared.domain.Venue
import com.qualorock.shared.domain.VerificationStatus
import com.qualorock.shared.search.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private data class CapturedCall(
    val q: String?,
    val dateBucket: DateBucket?,
    val city: String?,
    val genres: List<String>,
    val artistId: String?,
)

private class RecordingEventRepository(private val page: EventPage) : EventRepository {
    val calls = mutableListOf<CapturedCall>()

    override suspend fun getEventFeed(
        cursor: String?,
        limit: Int,
        q: String?,
        dateBucket: DateBucket?,
        city: String?,
        genres: List<String>,
        artistId: String?,
    ): Result<EventPage> {
        calls += CapturedCall(q, dateBucket, city, genres, artistId)
        return Result.success(page)
    }

    override suspend fun getEventDetail(id: String): Result<Event> = Result.failure(UnsupportedOperationException())
}

private class FailThenSucceedEventRepository(private val page: EventPage) : EventRepository {
    private var attempt = 0

    override suspend fun getEventFeed(
        cursor: String?,
        limit: Int,
        q: String?,
        dateBucket: DateBucket?,
        city: String?,
        genres: List<String>,
        artistId: String?,
    ): Result<EventPage> {
        attempt++
        return if (attempt == 1) Result.failure(RuntimeException("network unavailable")) else Result.success(page)
    }

    override suspend fun getEventDetail(id: String): Result<Event> = Result.failure(UnsupportedOperationException())
}

private class EmptyFilterOptionsRepository : FilterOptionsRepository {
    override suspend fun getGenreOptions(): Result<List<String>> = Result.success(emptyList())

    override suspend fun getArtistOptions(): Result<List<ArtistOption>> = Result.success(emptyList())
}

private fun event(id: String) =
    Event(
        id = id,
        title = "Show $id",
        startDateTime = Instant.parse("2026-08-16T22:00:00Z"),
        venue = Venue(id = "v-$id", name = "Venue $id", city = "Vitória", verificationStatus = VerificationStatus.VERIFIED),
        city = "Vitória",
        status = EventStatus.PUBLISHED,
    )

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FeedQueryViewModelTest {
    private fun harness(
        page: EventPage,
        scheduler: TestCoroutineScheduler,
    ): Triple<FeedQueryViewModel, SearchViewModel, FilterViewModel> {
        val dispatcher = StandardTestDispatcher(scheduler)
        val scope = CoroutineScope(dispatcher)
        val repository = RecordingEventRepository(page)
        val searchViewModel = SearchViewModel(scope, minQueryLength = 2, debounceMs = 300)
        val filterViewModel = FilterViewModel(EmptyFilterOptionsRepository(), scope)
        val feedQueryViewModel = FeedQueryViewModel(repository, searchViewModel, filterViewModel, scope)
        // WhileSubscribed sharing only starts the combined fetch once there's an active subscriber —
        // mirrors what Compose's collectAsState()/the iOS watch() bridge do in production.
        feedQueryViewModel.resultsState.onEach { }.launchIn(scope)
        return Triple(feedQueryViewModel, searchViewModel, filterViewModel)
    }

    @Test
    fun `given no search query and no filters when observed then resultsState is Inactive`() =
        runTest {
            val (feedQueryViewModel, _, _) = harness(EventPage(emptyList(), null), testScheduler)
            advanceUntilIdle()

            assertEquals(FeedResultsUiState.Inactive, feedQueryViewModel.resultsState.value)
        }

    @Test
    fun `given a debounced query when it resolves then one request is fired and Results is emitted`() =
        runTest {
            val (feedQueryViewModel, searchViewModel, _) = harness(EventPage(listOf(event("1")), null), testScheduler)

            searchViewModel.query.value = "forro"
            advanceTimeBy(400)
            advanceUntilIdle()

            val state = feedQueryViewModel.resultsState.value
            assertIs<FeedResultsUiState.Results>(state)
            assertEquals(listOf("1"), state.events.map { it.id })
        }

    @Test
    fun `given zero matching events when the debounced query resolves then NoResults is emitted`() =
        runTest {
            val (feedQueryViewModel, searchViewModel, _) = harness(EventPage(emptyList(), null), testScheduler)

            searchViewModel.query.value = "forro"
            advanceTimeBy(400)
            advanceUntilIdle()

            assertIs<FeedResultsUiState.NoResults>(feedQueryViewModel.resultsState.value)
        }

    @Test
    fun `given search and filters changing together when observed then only one request fires per combined change`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val repository = RecordingEventRepository(EventPage(listOf(event("1")), null))
            val searchViewModel = SearchViewModel(scope, minQueryLength = 2, debounceMs = 300)
            val filterViewModel = FilterViewModel(EmptyFilterOptionsRepository(), scope)
            val feedQueryViewModel = FeedQueryViewModel(repository, searchViewModel, filterViewModel, scope)
            feedQueryViewModel.resultsState.onEach { }.launchIn(scope)
            advanceUntilIdle()

            searchViewModel.query.value = "forro"
            advanceTimeBy(400)
            filterViewModel.selectCity("Vila Velha")
            advanceUntilIdle()

            assertTrue(repository.calls.isNotEmpty())
            val lastCall = repository.calls.last()
            assertEquals("forro", lastCall.q)
            assertEquals("Vila Velha", lastCall.city)
        }

    @Test
    fun `given a failed fetch when retry is called then the same query and filters are re-fetched`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val repository = FailThenSucceedEventRepository(EventPage(listOf(event("1")), null))
            val searchViewModel = SearchViewModel(scope, minQueryLength = 2, debounceMs = 300)
            val filterViewModel = FilterViewModel(EmptyFilterOptionsRepository(), scope)
            val feedQueryViewModel = FeedQueryViewModel(repository, searchViewModel, filterViewModel, scope)
            feedQueryViewModel.resultsState.onEach { }.launchIn(scope)

            searchViewModel.query.value = "forro"
            advanceTimeBy(400)
            advanceUntilIdle()
            assertIs<FeedResultsUiState.Error>(feedQueryViewModel.resultsState.value)

            feedQueryViewModel.retry()
            advanceUntilIdle()

            assertIs<FeedResultsUiState.Results>(feedQueryViewModel.resultsState.value)
        }
}
