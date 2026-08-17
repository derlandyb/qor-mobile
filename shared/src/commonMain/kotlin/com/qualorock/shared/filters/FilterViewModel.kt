package com.qualorock.shared.filters

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OptionsUiState<out T> {
    data object Loading : OptionsUiState<Nothing>

    data class Loaded<T>(val options: List<T>) : OptionsUiState<T>

    data class Error(val message: String) : OptionsUiState<Nothing>
}

/** Owns filter selection state and the genre/artist option lists (loaded once, per FILTER-003/004). */
class FilterViewModel(
    private val repository: FilterOptionsRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(FilterState())
    val state: StateFlow<FilterState> = _state.asStateFlow()

    private val _genreOptions = MutableStateFlow<OptionsUiState<String>>(OptionsUiState.Loading)
    val genreOptions: StateFlow<OptionsUiState<String>> = _genreOptions.asStateFlow()

    private val _artistOptions = MutableStateFlow<OptionsUiState<ArtistOption>>(OptionsUiState.Loading)
    val artistOptions: StateFlow<OptionsUiState<ArtistOption>> = _artistOptions.asStateFlow()

    init {
        scope.launch {
            _genreOptions.value =
                repository.getGenreOptions().fold(
                    onSuccess = { OptionsUiState.Loaded(it) },
                    onFailure = { OptionsUiState.Error(it.message ?: "genre_options_failed") },
                )
        }
        scope.launch {
            _artistOptions.value =
                repository.getArtistOptions().fold(
                    onSuccess = { OptionsUiState.Loaded(it) },
                    onFailure = { OptionsUiState.Error(it.message ?: "artist_options_failed") },
                )
        }
    }

    fun selectDateBucket(bucket: DateBucket?) = _state.update { it.copy(dateBucket = bucket) }

    fun selectCity(city: String?) = _state.update { it.copy(city = city) }

    fun toggleGenre(genre: String) =
        _state.update {
            it.copy(genres = if (genre in it.genres) it.genres - genre else it.genres + genre)
        }

    fun selectArtist(artist: ArtistOption?) = _state.update { it.copy(artist = artist) }

    fun removeChip(chip: FilterChip) =
        _state.update {
            when (chip) {
                is FilterChip.DateChip -> it.copy(dateBucket = null)
                is FilterChip.CityChip -> it.copy(city = null)
                is FilterChip.GenreChip -> it.copy(genres = emptySet())
                is FilterChip.ArtistChip -> it.copy(artist = null)
            }
        }

    fun clearAll() = _state.update { FilterState() }
}
