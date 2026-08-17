package com.qualorock.shared.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Owns debounce timing and the minimum-query-length guard once, shared by Compose and SwiftUI.
 * Does not fetch — the resulting [debouncedQuery] is consumed by
 * [com.qualorock.shared.filters.FeedQueryViewModel], the sole fetcher for the combined feed.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val scope: CoroutineScope,
    private val minQueryLength: Int = 2,
    private val debounceMs: Long = 300,
) {
    val query = MutableStateFlow("")

    private val _debouncedQuery = MutableStateFlow<String?>(null)
    val debouncedQuery: StateFlow<String?> = _debouncedQuery.asStateFlow()

    init {
        query
            .map { it.trim() }
            .debounce(debounceMs)
            .distinctUntilChanged()
            .onEach { q ->
                _debouncedQuery.value = if (q.isEmpty() || q.length < minQueryLength) null else q
            }
            .launchIn(scope)
    }
}
