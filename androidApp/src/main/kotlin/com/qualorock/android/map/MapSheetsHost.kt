package com.qualorock.android.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qualorock.shared.domain.Event

/**
 * Owns which overlay (single-event preview vs. multi-event/cluster list) is showing over the map, if any.
 * Split out from [MapScreen] so the sheet-presentation logic — the only part of marker-selection genuinely
 * testable without a live Google Maps render surface — can be exercised directly in Compose UI tests.
 */
@Composable
fun MapSheetsHost(
    previewEvent: Event?,
    clusterEvents: List<Event>?,
    onDismissPreview: () -> Unit,
    onOpenDetail: (Event) -> Unit,
    onDismissList: () -> Unit,
    onSelectFromList: (Event) -> Unit,
    onFavoriteClick: (Event) -> Unit,
    onShareClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (previewEvent != null) {
        MarkerPreviewSheet(
            event = previewEvent,
            onDismiss = onDismissPreview,
            onOpenDetail = onOpenDetail,
            onFavoriteClick = onFavoriteClick,
            onShareClick = onShareClick,
            modifier = modifier,
        )
    }
    if (clusterEvents != null) {
        MultiEventListSheet(
            events = clusterEvents,
            onDismiss = onDismissList,
            onSelect = onSelectFromList,
            onFavoriteClick = onFavoriteClick,
            onShareClick = onShareClick,
            modifier = modifier,
        )
    }
}
