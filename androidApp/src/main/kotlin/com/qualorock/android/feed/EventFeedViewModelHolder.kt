package com.qualorock.android.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qualorock.android.AppConfig
import com.qualorock.shared.data.KtorEventRepository
import com.qualorock.shared.feed.EventFeedViewModel
import com.qualorock.shared.filters.FeedQueryViewModel
import com.qualorock.shared.filters.FilterViewModel
import com.qualorock.shared.search.SearchViewModel

/**
 * Thin androidx ViewModel wrapper so the KMP view models survive Android configuration changes.
 * [filterViewModel] is shared with the Map tab (see [com.qualorock.android.SharedFilterViewModelHolder]),
 * not owned here, so filter state survives switching tabs (MAP-003 AC2).
 */
class EventFeedViewModelHolder(val filterViewModel: FilterViewModel) : ViewModel() {
    private val eventRepository = KtorEventRepository(baseUrl = AppConfig.API_BASE_URL)

    val eventFeedViewModel =
        EventFeedViewModel(
            repository = eventRepository,
            coroutineScope = viewModelScope,
        )

    val searchViewModel = SearchViewModel(scope = viewModelScope)

    val feedQueryViewModel =
        FeedQueryViewModel(
            repository = eventRepository,
            searchViewModel = searchViewModel,
            filterViewModel = filterViewModel,
            scope = viewModelScope,
        )

    companion object {
        fun factory(filterViewModel: FilterViewModel) =
            viewModelFactory {
                initializer { EventFeedViewModelHolder(filterViewModel) }
            }
    }
}
