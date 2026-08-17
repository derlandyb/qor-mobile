package com.qualorock.android.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qualorock.android.AppConfig
import com.qualorock.shared.data.KtorEventRepository
import com.qualorock.shared.feed.EventFeedViewModel
import com.qualorock.shared.filters.FeedQueryViewModel
import com.qualorock.shared.filters.FilterViewModel
import com.qualorock.shared.filters.KtorFilterOptionsRepository
import com.qualorock.shared.search.SearchViewModel

/** Thin androidx ViewModel wrapper so the KMP view models survive Android configuration changes. */
class EventFeedViewModelHolder : ViewModel() {
    private val eventRepository = KtorEventRepository(baseUrl = AppConfig.API_BASE_URL)

    val eventFeedViewModel =
        EventFeedViewModel(
            repository = eventRepository,
            coroutineScope = viewModelScope,
        )

    val searchViewModel = SearchViewModel(scope = viewModelScope)

    val filterViewModel =
        FilterViewModel(
            repository = KtorFilterOptionsRepository(baseUrl = AppConfig.API_BASE_URL),
            scope = viewModelScope,
        )

    val feedQueryViewModel =
        FeedQueryViewModel(
            repository = eventRepository,
            searchViewModel = searchViewModel,
            filterViewModel = filterViewModel,
            scope = viewModelScope,
        )
}
