package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.event.EventDetail
import domain.event.usecase.GetEventDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for [EventDetailScreen][br.com.qualorock.androidApp.ui.screen.EventDetailScreen] (A13, DISC-07–DISC-13). */
sealed class EventDetailUiState {
    data object Loading : EventDetailUiState()
    data class Content(val detail: EventDetail) : EventDetailUiState()

    /**
     * `EventRepository`/`GetEventDetails` carry no server-message contract to show verbatim,
     * same gap [HomeFeedUiState.Error] already documents — just one generic pt-BR failure case.
     */
    data object Error : EventDetailUiState()
}

/**
 * A13 — event detail (DISC-07–DISC-13). Thin loader over [GetEventDetails]: fetches once per
 * [load] call (keyed by the nav-graph's `eventId`, called from
 * [EventDetailScreen][br.com.qualorock.androidApp.ui.screen.EventDetailScreen] via
 * `LaunchedEffect`), with no pagination/polling machinery — unlike `HomeFeedViewModel` this
 * screen shows a single event, not a list, so there is nothing to page through or keep live.
 *
 * All Active/Cancelled/Ended branching lives in [EventDetail] itself (a sealed type per S12b) —
 * this class only fetches and surfaces failure; the `when` over it is entirely the screen's job.
 */
class EventDetailViewModel(private val getEventDetails: GetEventDetails) : ViewModel() {

    private val _uiState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun load(eventId: String) {
        _uiState.value = EventDetailUiState.Loading
        viewModelScope.launch {
            try {
                _uiState.value = EventDetailUiState.Content(getEventDetails.execute(eventId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = EventDetailUiState.Error
            }
        }
    }
}
