package com.qualorock.shared.map

import com.qualorock.shared.domain.Event
import com.qualorock.shared.filters.FilterState
import com.qualorock.shared.filters.FilterViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface MapMarkersUiState {
    data object Loading : MapMarkersUiState

    data class Loaded(val markers: List<Event>, val activeFilters: FilterState) : MapMarkersUiState

    data class Error(val message: String) : MapMarkersUiState
}

/**
 * Re-fetches map markers once per distinct [FilterViewModel] state change — the shared [FilterViewModel]
 * instance is lifted above the Feed/Map tab pair on each platform so filter state survives switching tabs
 * (MAP-003 AC2). Projection-dependent concerns (clustering, camera bounds, empty-viewport detection) are
 * intentionally NOT modeled here — see [MapDisplayStateDeriver], owned per-platform by the native map layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapQueryViewModel(
    private val repository: MapRepository,
    filterViewModel: FilterViewModel,
    scope: CoroutineScope,
) {
    private val retryTrigger = MutableStateFlow(0)

    val markersState: StateFlow<MapMarkersUiState> =
        combine(filterViewModel.state, retryTrigger) { filters, _ -> filters }
            .flatMapLatest { filters ->
                flow {
                    emit(MapMarkersUiState.Loading)
                    val result =
                        repository.getMapMarkers(
                            dateBucket = filters.dateBucket,
                            city = filters.city,
                            genres = filters.genres.toList(),
                            artistId = filters.artist?.id,
                        )
                    emit(
                        result.fold(
                            onSuccess = { MapMarkersUiState.Loaded(it, filters) },
                            onFailure = { MapMarkersUiState.Error(it.message ?: "map_markers_failed") },
                        ),
                    )
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), MapMarkersUiState.Loading)

    /** Re-issues the fetch for the current filters — for [MapMarkersUiState.Error]'s retry action. */
    fun retry() = retryTrigger.update { it + 1 }
}
