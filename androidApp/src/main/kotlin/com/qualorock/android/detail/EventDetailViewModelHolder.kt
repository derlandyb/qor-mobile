package com.qualorock.android.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qualorock.android.AppConfig
import com.qualorock.shared.data.KtorEventRepository
import com.qualorock.shared.detail.EventDetailViewModel

/** Thin androidx ViewModel wrapper so the KMP [EventDetailViewModel] survives Android configuration changes. */
class EventDetailViewModelHolder(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val repository = KtorEventRepository(baseUrl = AppConfig.API_BASE_URL)

    val eventDetailViewModel =
        EventDetailViewModel(
            repository = repository,
            eventId = checkNotNull(savedStateHandle["eventId"]),
            coroutineScope = viewModelScope,
        ).also { it.load() }

    override fun onCleared() {
        repository.close()
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    EventDetailViewModelHolder(createSavedStateHandle())
                }
            }
    }
}
