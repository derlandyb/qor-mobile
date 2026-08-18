package com.qualorock.android.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.qualorock.android.feed.EventCard
import com.qualorock.shared.domain.Event

/**
 * Overlay preview shown when a single-event marker is tapped (MAP-002/009) — reuses [EventCard]'s styling,
 * plus an explicit "open full detail" action. The map stays visible/interactive underneath (this is a sheet,
 * not a navigation event), so dismissing it returns to the unobstructed map without losing pan/zoom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerPreviewSheet(
    event: Event,
    onDismiss: () -> Unit,
    onOpenDetail: (Event) -> Unit,
    onFavoriteClick: (Event) -> Unit,
    onShareClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier.testTag("marker_preview_sheet")) {
        Column {
            EventCard(event = event, onClick = onOpenDetail, onFavoriteClick = onFavoriteClick, onShareClick = onShareClick)
            Button(
                onClick = { onOpenDetail(event) },
                modifier = Modifier.padding(horizontal = 16.dp).testTag("open_detail_button"),
            ) {
                Text("Ver detalhes do evento")
            }
        }
    }
}
