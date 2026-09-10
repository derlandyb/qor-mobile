package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.enum.City
import domain.event.Event
import domain.event.usecase.ListUpcomingEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for [HubScreen][br.com.qualorock.androidApp.ui.screen.HubScreen] (T33, HUB-01..04). */
sealed class HubUiState {
    data object Loading : HubUiState()
    data class Content(val events: List<Event>) : HubUiState()

    /** HUB-03 — a city with zero published events at time of viewing; a valid state, not a failure. */
    data object Empty : HubUiState()

    /** `ListUpcomingEvents`/`EventRepository` carry no server-message contract, same gap [HomeFeedUiState.Error] documents. */
    data object Error : HubUiState()
}

/**
 * T33 — one city's Hub (HUB-01..04): calls the existing `GET /events?city=` filter via
 * [listUpcomingEvents] (design.md's Tech Decisions: "The system SHALL NOT call a dedicated
 * hub-aggregation endpoint" — reuses the same list surface [HomeFeedViewModel]/[ExploreViewModel]
 * already call, no new shared-module slice needed). [load] takes [City] as a call argument
 * (mirroring [EventDetailViewModel.load]'s `eventId` parameter) rather than a constructor
 * dependency, so the nav graph can pass the route's `city` arg without a parameterized Koin
 * definition — per the spec's Edge Case, that route arg is the *only* source of the city, no
 * stored preference is ever consulted.
 */
class HubViewModel(private val listUpcomingEvents: ListUpcomingEvents) : ViewModel() {

    private val _uiState = MutableStateFlow<HubUiState>(HubUiState.Loading)
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun load(city: City) {
        _uiState.value = HubUiState.Loading
        viewModelScope.launch {
            try {
                val page = listUpcomingEvents.execute(city = city)
                _uiState.value = if (page.events.isEmpty()) HubUiState.Empty else HubUiState.Content(page.events)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = HubUiState.Error
            }
        }
    }
}
