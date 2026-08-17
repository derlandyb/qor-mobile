package com.qualorock.shared.filters

import com.qualorock.shared.data.EventRepository
import com.qualorock.shared.domain.Event
import com.qualorock.shared.search.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

sealed interface FeedResultsUiState {
    data object Inactive : FeedResultsUiState

    data object Loading : FeedResultsUiState

    data class Results(val events: List<Event>, val activeFilters: FilterState, val q: String) : FeedResultsUiState

    data class NoResults(val activeFilters: FilterState, val q: String) : FeedResultsUiState

    data class Error(val message: String) : FeedResultsUiState
}

/**
 * Combines [SearchViewModel]'s debounced query with [FilterViewModel]'s selection into the single
 * fetch that drives the Feed's combined-results list — the sole caller of [EventRepository.getEventFeed]
 * for search/filters, so the two never race the same endpoint (FILTER-005 AC3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedQueryViewModel(
    private val repository: EventRepository,
    searchViewModel: SearchViewModel,
    filterViewModel: FilterViewModel,
    scope: CoroutineScope,
) {
    val resultsState: StateFlow<FeedResultsUiState> =
        combine(searchViewModel.debouncedQuery, filterViewModel.state) { q, filters -> (q ?: "") to filters }
            .distinctUntilChanged()
            .flatMapLatest { (q, filters) ->
                flow {
                    if (q.isBlank() && filters.isEmpty) {
                        emit(FeedResultsUiState.Inactive)
                    } else {
                        emit(FeedResultsUiState.Loading)
                        val result =
                            repository.getEventFeed(
                                q = q.ifBlank { null },
                                dateBucket = filters.dateBucket,
                                city = filters.city,
                                genres = filters.genres.toList(),
                                artistId = filters.artist?.id,
                            )
                        emit(
                            result.fold(
                                onSuccess = { page ->
                                    if (page.events.isEmpty()) {
                                        FeedResultsUiState.NoResults(filters, q)
                                    } else {
                                        FeedResultsUiState.Results(page.events, filters, q)
                                    }
                                },
                                onFailure = { FeedResultsUiState.Error(it.message ?: "filtered_feed_failed") },
                            ),
                        )
                    }
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), FeedResultsUiState.Inactive)
}
