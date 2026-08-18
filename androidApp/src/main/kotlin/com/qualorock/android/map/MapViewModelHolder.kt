package com.qualorock.android.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qualorock.android.AppConfig
import com.qualorock.shared.filters.FilterViewModel
import com.qualorock.shared.map.KtorMapRepository
import com.qualorock.shared.map.MapQueryViewModel

/** Thin androidx ViewModel wrapper so the KMP [MapQueryViewModel] survives Android configuration changes. */
class MapViewModelHolder(filterViewModel: FilterViewModel) : ViewModel() {
    private val repository = KtorMapRepository(baseUrl = AppConfig.API_BASE_URL)

    val mapQueryViewModel =
        MapQueryViewModel(
            repository = repository,
            filterViewModel = filterViewModel,
            scope = viewModelScope,
        )

    override fun onCleared() {
        repository.close()
    }

    companion object {
        fun factory(filterViewModel: FilterViewModel) =
            viewModelFactory {
                initializer { MapViewModelHolder(filterViewModel) }
            }
    }
}
