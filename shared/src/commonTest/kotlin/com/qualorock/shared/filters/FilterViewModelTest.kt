package com.qualorock.shared.filters

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeFilterOptionsRepository(
    private val genres: Result<List<String>> = Result.success(listOf("Rock", "Samba")),
    private val artists: Result<List<ArtistOption>> = Result.success(listOf(ArtistOption("1", "Jorge & the Band"))),
) : FilterOptionsRepository {
    override suspend fun getGenreOptions(): Result<List<String>> = genres

    override suspend fun getArtistOptions(): Result<List<ArtistOption>> = artists
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FilterViewModelTest {
    private fun newViewModel(repository: FilterOptionsRepository = FakeFilterOptionsRepository()): FilterViewModel {
        val dispatcher = StandardTestDispatcher()
        val scope = CoroutineScope(dispatcher)
        return FilterViewModel(repository, scope)
    }

    @Test
    fun `given a genre when toggleGenre is called twice then it is added then removed from the set`() =
        runTest {
            val viewModel = newViewModel()

            viewModel.toggleGenre("Rock")
            assertEquals(setOf("Rock"), viewModel.state.value.genres)

            viewModel.toggleGenre("Rock")
            assertEquals(emptySet(), viewModel.state.value.genres)
        }

    @Test
    fun `given a prior city selection when selectCity is called again then it replaces rather than accumulates`() =
        runTest {
            val viewModel = newViewModel()

            viewModel.selectCity("Vitória")
            viewModel.selectCity("Vila Velha")

            assertEquals("Vila Velha", viewModel.state.value.city)
        }

    @Test
    fun `given a prior date bucket selection when selectDateBucket is called again then it replaces rather than accumulates`() =
        runTest {
            val viewModel = newViewModel()

            viewModel.selectDateBucket(DateBucket.HOJE)
            viewModel.selectDateBucket(DateBucket.AMANHA)

            assertEquals(DateBucket.AMANHA, viewModel.state.value.dateBucket)
        }

    @Test
    fun `given multiple active filters when removeChip is called for one type then only that type is cleared`() =
        runTest {
            val viewModel = newViewModel()
            viewModel.selectCity("Vila Velha")
            viewModel.selectDateBucket(DateBucket.HOJE)

            viewModel.removeChip(FilterChip.CityChip("Vila Velha"))

            assertNull(viewModel.state.value.city)
            assertEquals(DateBucket.HOJE, viewModel.state.value.dateBucket)
        }

    @Test
    fun `given several active filters when clearAll is called then state resets to empty`() =
        runTest {
            val viewModel = newViewModel()
            viewModel.selectCity("Vila Velha")
            viewModel.toggleGenre("Rock")

            viewModel.clearAll()

            assertTrue(viewModel.state.value.isEmpty)
        }

    @Test
    fun `given a repository when the view model is created then genre and artist options load`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val viewModel = FilterViewModel(FakeFilterOptionsRepository(), scope)

            advanceUntilIdle()

            assertEquals(OptionsUiState.Loaded(listOf("Rock", "Samba")), viewModel.genreOptions.value)
            assertEquals(
                OptionsUiState.Loaded(listOf(ArtistOption("1", "Jorge & the Band"))),
                viewModel.artistOptions.value,
            )
        }

    @Test
    fun `given a failing repository when the view model is created then options become an Error state`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val repository =
                FakeFilterOptionsRepository(
                    genres = Result.failure(RuntimeException("network unavailable")),
                    artists = Result.failure(RuntimeException("network unavailable")),
                )
            val viewModel = FilterViewModel(repository, scope)

            advanceUntilIdle()

            assertTrue(viewModel.genreOptions.value is OptionsUiState.Error)
            assertTrue(viewModel.artistOptions.value is OptionsUiState.Error)
        }
}
