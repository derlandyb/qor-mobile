package com.qualorock.android.map

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.qualorock.android.feed.EventCard
import com.qualorock.shared.domain.Event

/**
 * Shown when a cluster marker is tapped — whether it groups multiple events at the same venue, or several
 * nearby venues (MAP-004/010). Both cases resolve to the same list-on-tap: every constituent [Event], each
 * tappable to its own [MarkerPreviewSheet]/detail — no two-step zoom-then-list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiEventListSheet(
    events: List<Event>,
    onDismiss: () -> Unit,
    onSelect: (Event) -> Unit,
    onFavoriteClick: (Event) -> Unit,
    onShareClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier.testTag("multi_event_list_sheet")) {
        LazyColumn {
            items(events, key = { it.id }) { event ->
                EventCard(event = event, onClick = onSelect, onFavoriteClick = onFavoriteClick, onShareClick = onShareClick)
            }
        }
    }
}
