package com.qualorock.shared.detail

import com.qualorock.shared.data.EventNotFoundException
import com.qualorock.shared.data.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventDetailViewModel(
    private val repository: EventRepository,
    private val eventId: String,
    private val coroutineScope: CoroutineScope,
) {
    private val _state = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    val state: StateFlow<EventDetailUiState> = _state.asStateFlow()

    fun load() {
        _state.update { EventDetailUiState.Loading }
        coroutineScope.launch {
            repository.getEventDetail(eventId)
                .onSuccess { event -> _state.update { EventDetailUiState.Loaded(event) } }
                .onFailure { error ->
                    _state.update {
                        if (error is EventNotFoundException) {
                            EventDetailUiState.NotFound
                        } else {
                            EventDetailUiState.LoadError(error.message ?: "Erro desconhecido")
                        }
                    }
                }
        }
    }

    fun retry() = load()
}
