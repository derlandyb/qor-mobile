package com.qualorock.shared.feed

enum class FeedError {
    INITIAL_LOAD,
    LOAD_MORE,
}

data class EventFeedUiState(
    val groupedEvents: List<DateGroup> = emptyList(),
    val isLoadingInitial: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: FeedError? = null,
    val endReached: Boolean = false,
) {
    val isEmpty: Boolean get() = groupedEvents.isEmpty() && !isLoadingInitial && error == null
}
