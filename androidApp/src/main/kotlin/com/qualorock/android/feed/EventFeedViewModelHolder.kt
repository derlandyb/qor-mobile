package com.qualorock.android.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qualorock.android.AppConfig
import com.qualorock.shared.data.KtorEventRepository
import com.qualorock.shared.feed.EventFeedViewModel

/** Thin androidx ViewModel wrapper so the KMP [EventFeedViewModel] survives Android configuration changes. */
class EventFeedViewModelHolder : ViewModel() {
    val eventFeedViewModel =
        EventFeedViewModel(
            repository = KtorEventRepository(baseUrl = AppConfig.API_BASE_URL),
            coroutineScope = viewModelScope,
        )
}
