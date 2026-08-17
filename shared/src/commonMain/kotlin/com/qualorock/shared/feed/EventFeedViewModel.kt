package com.qualorock.shared.feed

import com.qualorock.shared.data.EventRepository
import com.qualorock.shared.domain.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

class EventFeedViewModel(
    private val repository: EventRepository,
    private val coroutineScope: CoroutineScope,
) {
    private val _state = MutableStateFlow(EventFeedUiState())
    val state: StateFlow<EventFeedUiState> = _state.asStateFlow()

    private val loadedEvents = mutableListOf<Event>()
    private var nextCursor: String? = null
    private var hasLoadedOnce = false

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingInitial || current.isLoadingMore || current.endReached) return

        val isInitial = !hasLoadedOnce
        _state.update { it.copy(isLoadingInitial = isInitial, isLoadingMore = !isInitial, error = null) }

        coroutineScope.launch {
            repository.getEventFeed(cursor = nextCursor, limit = PAGE_SIZE)
                .onSuccess { page ->
                    hasLoadedOnce = true
                    nextCursor = page.nextCursor
                    loadedEvents.addAll(page.events)
                    _state.update {
                        it.copy(
                            groupedEvents = DateGrouper.group(loadedEvents),
                            isLoadingInitial = false,
                            isLoadingMore = false,
                            error = null,
                            endReached = page.nextCursor == null,
                        )
                    }
                }
                .onFailure {
                    _state.update { current ->
                        current.copy(
                            isLoadingInitial = false,
                            isLoadingMore = false,
                            error = if (isInitial) FeedError.INITIAL_LOAD else FeedError.LOAD_MORE,
                        )
                    }
                }
        }
    }

    fun retry() {
        _state.update { it.copy(error = null) }
        loadNextPage()
    }
}
