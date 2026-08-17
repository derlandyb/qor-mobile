package com.qualorock.android.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qualorock.android.R
import com.qualorock.shared.domain.Event
import com.qualorock.shared.feed.EventFeedUiState
import com.qualorock.shared.feed.FeedError

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventFeedScreen(
    state: EventFeedUiState,
    onLoadNextPage: () -> Unit,
    onRetry: () -> Unit,
    onEventClick: (Event) -> Unit,
    onFavoriteClick: (Event) -> Unit,
    onShareClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoadingInitial -> InitialLoadingState(modifier)
        state.error == FeedError.INITIAL_LOAD -> InitialErrorState(onRetry, modifier)
        state.isEmpty -> EmptyState(modifier)
        else -> {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val shouldLoadMore by remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val totalItems = listState.layoutInfo.totalItemsCount
                    totalItems > 0 && lastVisible >= totalItems - 3
                }
            }

            androidx.compose.runtime.LaunchedEffect(shouldLoadMore, state.isLoadingMore, state.error, state.endReached) {
                if (shouldLoadMore && !state.isLoadingMore && state.error != FeedError.LOAD_MORE && !state.endReached) {
                    onLoadNextPage()
                }
            }

            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                state.groupedEvents.forEach { group ->
                    stickyHeader(key = "header-${group.label}") {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Text(
                                text = group.label,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            )
                        }
                    }
                    itemsIndexed(group.events, key = { _, event -> event.id }) { _, event ->
                        EventCard(
                            event = event,
                            onClick = onEventClick,
                            onFavoriteClick = onFavoriteClick,
                            onShareClick = onShareClick,
                        )
                    }
                }

                if (state.isLoadingMore) {
                    item(key = "loading-more") {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (state.error == FeedError.LOAD_MORE) {
                    item(key = "retry-row") {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Button(onClick = onRetry) {
                                Text(text = stringResource(id = R.string.feed_retry))
                            }
                        }
                    }
                }

                if (state.endReached && state.error != FeedError.LOAD_MORE) {
                    item(key = "end-reached") {
                        Text(
                            text = stringResource(id = R.string.feed_end_reached),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun InitialErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(id = R.string.feed_error_title), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = stringResource(id = R.string.feed_retry))
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = stringResource(id = R.string.feed_empty_title), style = MaterialTheme.typography.titleMedium)
    }
}
