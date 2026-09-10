package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.event.Event
import domain.favorite.usecase.ListFavorites
import domain.favorite.usecase.ToggleFavorite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for [FavoritesScreen][br.com.qualorock.androidApp.ui.screen.FavoritesScreen] (T31, FAVUI-01/02/04/05). */
sealed class FavoritesUiState {
    data object Loading : FavoritesUiState()
    data class Content(val events: List<Event>) : FavoritesUiState()
    data object Empty : FavoritesUiState()

    /**
     * `ListFavorites`/`FavoriteRepository` carry no server-message contract to show verbatim,
     * same gap [HomeFeedUiState.Error] already documents — just one generic pt-BR failure case.
     */
    data object Error : FavoritesUiState()
}

/**
 * T31 — Favoritos tab (FAVUI-01/02/04/05): loads the fan's favorited events via [listFavorites]
 * and lets the fan un-favorite directly from this list via [onUnfavorite] (FAVUI-04).
 *
 * **Un-favorite is optimistic with rollback (design.md's Error Handling Strategy + the
 * spec's optimistic-toggle race Edge Case):** [onUnfavorite] removes the event from
 * [uiState] immediately, then calls [toggleFavorite]. If the call fails, or if the server's
 * response says the event is *still* favorited (the race case — some other action
 * re-favorited it before this toggle's response came back), the event is restored — the UI
 * always resolves to the server's actual final state, never the optimistic guess.
 */
class FavoritesViewModel(
    private val listFavorites: ListFavorites,
    private val toggleFavorite: ToggleFavorite,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = FavoritesUiState.Loading
        viewModelScope.launch {
            listFavorites.execute().fold(
                onSuccess = { events ->
                    _uiState.value = if (events.isEmpty()) FavoritesUiState.Empty else FavoritesUiState.Content(events)
                },
                onFailure = { _uiState.value = FavoritesUiState.Error },
            )
        }
    }

    /** FAVUI-04 — un-favorites [eventId] from the list itself, no full reload. See class KDoc for the rollback contract. */
    fun onUnfavorite(eventId: String) {
        val state = _uiState.value
        if (state !is FavoritesUiState.Content) return
        val previousEvents = state.events
        val removed = previousEvents.filterNot { it.id == eventId }
        _uiState.value = if (removed.isEmpty()) FavoritesUiState.Empty else FavoritesUiState.Content(removed)

        viewModelScope.launch {
            toggleFavorite.execute(eventId).fold(
                onSuccess = { stillFavorited -> if (stillFavorited) restore(eventId, previousEvents) },
                onFailure = { restore(eventId, previousEvents) },
            )
        }
    }

    /** Reinserts [eventId] (looked up from [previousEvents]) into the current list, unless it's already there. */
    private fun restore(eventId: String, previousEvents: List<Event>) {
        val restoredEvent = previousEvents.firstOrNull { it.id == eventId } ?: return
        val currentEvents = (_uiState.value as? FavoritesUiState.Content)?.events.orEmpty()
        if (currentEvents.any { it.id == eventId }) return
        _uiState.value = FavoritesUiState.Content(currentEvents + restoredEvent)
    }
}
