package br.com.qualorock.androidApp.ui.viewmodel

import domain.enum.City
import domain.event.PollingCoordinator
import domain.event.usecase.ListUpcomingEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A12 — Explore tab (DISC-14–DISC-18): the same public event list as [HomeFeedScreen][br.com.qualorock.androidApp.ui.screen.HomeFeedScreen]/
 * [HomeFeedViewModel], but with city/genre filters exposed on top, as its own BottomNav
 * destination/back-stack entry per the task breakdown (the underlying event-discovery spec treats
 * filtering as the same list surface with controls added, not a separate concept).
 *
 * Subclasses [HomeFeedViewModel] rather than duplicating its pagination/polling/empty-state
 * machinery — see that class's own KDoc for why it was made `open`/given a `protected`
 * [HomeFeedViewModel.applyFilters] hook. This class only adds the filter *selection* state
 * ([selectedCity]/[selectedGenre]) and the three DISC-14/DISC-15/DISC-17 entry points that toggle
 * it and delegate to [HomeFeedViewModel.applyFilters]; [uiState] (inherited, unmodified) keeps
 * driving the list/loading/empty/error rendering exactly as it does for [HomeFeedScreen][br.com.qualorock.androidApp.ui.screen.HomeFeedScreen].
 *
 * Both filters are AND-combined (DISC-16) simply by always passing the *current* pair of
 * [selectedCity]/[selectedGenre] to [HomeFeedViewModel.applyFilters] — selecting one filter never
 * discards the other.
 */
class ExploreViewModel(
    listUpcomingEvents: ListUpcomingEvents,
    pollingCoordinator: PollingCoordinator,
) : HomeFeedViewModel(listUpcomingEvents, pollingCoordinator) {

    private val _selectedCity = MutableStateFlow<City?>(null)
    val selectedCity: StateFlow<City?> = _selectedCity.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    /** DISC-14 — toggles [city]: selecting the already-active city clears it (back to unfiltered). */
    fun onCitySelected(city: City) {
        _selectedCity.value = if (_selectedCity.value == city) null else city
        applyFilters(_selectedCity.value, _selectedGenre.value)
    }

    /** DISC-15 — toggles [genre]: selecting the already-active genre clears it (back to unfiltered). */
    fun onGenreSelected(genre: String) {
        _selectedGenre.value = if (_selectedGenre.value == genre) null else genre
        applyFilters(_selectedCity.value, _selectedGenre.value)
    }

    /** DISC-17 — clears both filters, returning to the default unfiltered soonest-first list. */
    fun onClearFilters() {
        _selectedCity.value = null
        _selectedGenre.value = null
        applyFilters(null, null)
    }
}
