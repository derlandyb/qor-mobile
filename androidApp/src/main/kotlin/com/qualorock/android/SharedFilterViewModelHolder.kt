package com.qualorock.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qualorock.shared.filters.FilterViewModel
import com.qualorock.shared.filters.KtorFilterOptionsRepository

/**
 * Owns the single [FilterViewModel] instance shared by the Feed and Map tabs, so active filters survive
 * switching tabs (MAP-003 AC2). Obtained once at the top of [MainActivity]'s composable tree (Activity-scoped,
 * outside the NavHost's per-route ViewModelStore) rather than by each tab's own holder.
 */
class SharedFilterViewModelHolder : ViewModel() {
    val filterViewModel =
        FilterViewModel(
            repository = KtorFilterOptionsRepository(baseUrl = AppConfig.API_BASE_URL),
            scope = viewModelScope,
        )
}
