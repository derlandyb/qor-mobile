package com.qualorock.shared.detail

import com.qualorock.shared.domain.Event

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState

    data class Loaded(val event: Event) : EventDetailUiState

    data object NotFound : EventDetailUiState

    data class LoadError(val message: String) : EventDetailUiState
}
