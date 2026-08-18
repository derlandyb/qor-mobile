package com.qualorock.shared.filters

import com.qualorock.shared.data.KtorEventRepository
import com.qualorock.shared.feed.Closeable
import com.qualorock.shared.search.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Swift-friendly facade combining [SearchViewModel], [FilterViewModel] and [FeedQueryViewModel] —
 * Kotlin's [kotlinx.coroutines.flow.StateFlow] isn't directly consumable from Swift.
 */
class IosFeedQueryViewModel(baseUrl: String) {
    private val lifecycleJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + lifecycleJob)

    private val searchViewModel = SearchViewModel(scope)

    /** Swift calls filter-selection methods (`selectCity`, `toggleGenre`, ...) directly on this. */
    val filterViewModel = FilterViewModel(KtorFilterOptionsRepository(baseUrl), scope)
    private val feedQueryViewModel =
        FeedQueryViewModel(KtorEventRepository(baseUrl), searchViewModel, filterViewModel, scope)

    fun setQuery(query: String) {
        searchViewModel.query.value = query
    }

    fun retryResults() = feedQueryViewModel.retry()

    fun watchResults(onState: (FeedResultsUiState) -> Unit): Closeable = watch(feedQueryViewModel.resultsState, onState)

    fun watchFilterState(onState: (FilterState) -> Unit): Closeable = watch(filterViewModel.state, onState)

    fun watchGenreOptions(onState: (OptionsUiState<String>) -> Unit): Closeable = watch(filterViewModel.genreOptions, onState)

    fun watchArtistOptions(onState: (OptionsUiState<ArtistOption>) -> Unit): Closeable = watch(filterViewModel.artistOptions, onState)

    private fun <T> watch(
        flow: kotlinx.coroutines.flow.StateFlow<T>,
        onState: (T) -> Unit,
    ): Closeable {
        val watchJob = Job(lifecycleJob)
        flow.onEach { onState(it) }.launchIn(CoroutineScope(Dispatchers.Main + watchJob))
        return object : Closeable {
            override fun close() {
                watchJob.cancel()
            }
        }
    }

    fun close() = lifecycleJob.cancel()
}
