package com.qualorock.shared.map

import com.qualorock.shared.domain.Event
import com.qualorock.shared.domain.EventStatus
import com.qualorock.shared.domain.Venue
import com.qualorock.shared.domain.VerificationStatus
import com.qualorock.shared.filters.ArtistOption
import com.qualorock.shared.filters.DateBucket
import com.qualorock.shared.filters.FilterOptionsRepository
import com.qualorock.shared.filters.FilterViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private data class CapturedCall(
    val dateBucket: DateBucket?,
    val city: String?,
    val genres: List<String>,
    val artistId: String?,
)

private class RecordingMapRepository(private val markers: List<Event>) : MapRepository {
    val calls = mutableListOf<CapturedCall>()

    override suspend fun getMapMarkers(
        dateBucket: DateBucket?,
        city: String?,
        genres: List<String>,
        artistId: String?,
    ): Result<List<Event>> {
        calls += CapturedCall(dateBucket, city, genres, artistId)
        return Result.success(markers)
    }
}

private class FailThenSucceedMapRepository(private val markers: List<Event>) : MapRepository {
    private var attempt = 0

    override suspend fun getMapMarkers(
        dateBucket: DateBucket?,
        city: String?,
        genres: List<String>,
        artistId: String?,
    ): Result<List<Event>> {
        attempt++
        return if (attempt == 1) Result.failure(RuntimeException("network unavailable")) else Result.success(markers)
    }
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
class MapQueryViewModelTest {
    @Test
    fun `given no filters when observed then markers are fetched once and Loaded is emitted`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val repository = RecordingMapRepository(listOf(event("1")))
            val filterViewModel = FilterViewModel(EmptyFilterOptionsRepository(), scope)
            val viewModel = MapQueryViewModel(repository, filterViewModel, scope)
            viewModel.markersState.onEach { }.launchIn(scope)

            advanceUntilIdle()

            val state = viewModel.markersState.value
            assertIs<MapMarkersUiState.Loaded>(state)
            assertEquals(listOf("1"), state.markers.map { it.id })
            assertEquals(1, repository.calls.size)
        }

    @Test
    fun `given a filter change when observed then exactly one re-fetch fires and activeFilters is carried through`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val repository = RecordingMapRepository(listOf(event("1")))
            val filterViewModel = FilterViewModel(EmptyFilterOptionsRepository(), scope)
            val viewModel = MapQueryViewModel(repository, filterViewModel, scope)
            viewModel.markersState.onEach { }.launchIn(scope)
            advanceUntilIdle()

            filterViewModel.selectCity("Vila Velha")
            advanceUntilIdle()

            assertEquals(2, repository.calls.size)
            assertEquals("Vila Velha", repository.calls.last().city)
            val state = viewModel.markersState.value
            assertIs<MapMarkersUiState.Loaded>(state)
            assertEquals("Vila Velha", state.activeFilters.city)
        }

    @Test
    fun `given a fetch failure when observed then Error is emitted`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val repository = FailThenSucceedMapRepository(listOf(event("1")))
            val filterViewModel = FilterViewModel(EmptyFilterOptionsRepository(), scope)
            val viewModel = MapQueryViewModel(repository, filterViewModel, scope)
            viewModel.markersState.onEach { }.launchIn(scope)

            advanceUntilIdle()

            assertIs<MapMarkersUiState.Error>(viewModel.markersState.value)
        }

    @Test
    fun `given a failed fetch when retry is called then markers are re-fetched and Loaded is emitted`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val repository = FailThenSucceedMapRepository(listOf(event("1")))
            val filterViewModel = FilterViewModel(EmptyFilterOptionsRepository(), scope)
            val viewModel = MapQueryViewModel(repository, filterViewModel, scope)
            viewModel.markersState.onEach { }.launchIn(scope)
            advanceUntilIdle()
            assertIs<MapMarkersUiState.Error>(viewModel.markersState.value)

            viewModel.retry()
            advanceUntilIdle()

            assertIs<MapMarkersUiState.Loaded>(viewModel.markersState.value)
        }
}
