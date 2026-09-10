package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.qualorock.androidApp.ui.screen.MapPin
import br.com.qualorock.androidApp.ui.screen.toMapPins
import domain.enum.City
import domain.event.MapBounds
import domain.event.usecase.GetMapEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for [MapScreen][br.com.qualorock.androidApp.ui.screen.MapScreen] (T32, MAPUI-01..04). */
sealed class MapUiState {
    data object Loading : MapUiState()

    /**
     * A zero-pin [pins] list is a *valid* rendering of this state (MAPUI-04/design.md's Error
     * Handling Strategy: "zero geocoded events in view" is not an error) — [MapScreen] itself
     * decides whether to layer an [br.com.qualorock.androidApp.ui.components.EmptyState] hint on
     * top, this class doesn't need a separate `Empty` case for it.
     */
    data class Content(val pins: List<MapPin>) : MapUiState()

    /** `GetMapEvents`/`EventRepository` carry no server-message contract, same gap [HomeFeedUiState.Error] documents. */
    data object Error : MapUiState()
}

/**
 * T32 — Map screen (MAPUI-01..04): loads geocoded events via [getMapEvents] either by [City]
 * (city mode) or [MapBounds] (viewport mode, MAPUI-03's pan/zoom re-query), converting the
 * result to [MapPin]s via [toMapPins] (drops any null-coordinate event defensively).
 */
class MapViewModel(private val getMapEvents: GetMapEvents) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** City-mode query (initial load default, or an explicit city filter). */
    fun loadByCity(city: City) {
        _uiState.value = MapUiState.Loading
        viewModelScope.launch {
            getMapEvents.execute(city = city).fold(
                onSuccess = { events -> _uiState.value = MapUiState.Content(toMapPins(events)) },
                onFailure = { _uiState.value = MapUiState.Error },
            )
        }
    }

    /** MAPUI-03 — viewport-mode query, re-run whenever the map's visible bounds change (pan/zoom). */
    fun loadByBounds(bounds: MapBounds) {
        _uiState.value = MapUiState.Loading
        viewModelScope.launch {
            getMapEvents.execute(bounds = bounds).fold(
                onSuccess = { events -> _uiState.value = MapUiState.Content(toMapPins(events)) },
                onFailure = { _uiState.value = MapUiState.Error },
            )
        }
    }
}
