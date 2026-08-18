package com.qualorock.android.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.qualorock.shared.domain.Event
import com.qualorock.shared.map.MapBounds
import com.qualorock.shared.map.MapDisplayStateDeriver
import com.qualorock.shared.map.MapMarkersUiState

/**
 * The Map tab (MAP-001/005): a Grande-Vitória-wide, zero-permission view of upcoming events, applying the
 * same filter state as Feed (lifted above this screen — see [com.qualorock.android.SharedFilterViewModelHolder]).
 * Marker/cluster tap state lives in [MapSheetsHost]; empty/error/no-results banners in [MapDisplayStateBanner].
 */
@Composable
fun MapScreen(
    markersState: MapMarkersUiState,
    onOpenEventDetails: (String) -> Unit,
    onRetry: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var previewEvent by remember { mutableStateOf<Event?>(null) }
    var clusterEvents by remember { mutableStateOf<List<Event>?>(null) }
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(GrandeVitoriaBounds.googleMaps.center, 10f)
        }

    Scaffold(modifier = modifier.testTag("map_screen")) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (markersState) {
                is MapMarkersUiState.Loading -> MapLoadingBanner(modifier = Modifier.align(Alignment.Center))
                is MapMarkersUiState.Error ->
                    MapErrorBanner(
                        message = markersState.message,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                is MapMarkersUiState.Loaded -> {
                    val displayState =
                        MapDisplayStateDeriver.derive(
                            markers = markersState.markers,
                            visibleBounds = visibleBounds(cameraPositionState) ?: GrandeVitoriaBounds.shared,
                            activeFilters = markersState.activeFilters,
                        )
                    GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
                        Clustering(
                            items = markersState.markers.map { EventClusterItem(it) },
                            onClusterItemClick = { item ->
                                previewEvent = item.event
                                true
                            },
                            onClusterClick = { cluster ->
                                clusterEvents = cluster.items.map { it.event }
                                true
                            },
                        )
                    }
                    MapDisplayStateBanner(
                        displayState = displayState,
                        onClearFilters = onClearFilters,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            MapSheetsHost(
                previewEvent = previewEvent,
                clusterEvents = clusterEvents,
                onDismissPreview = { previewEvent = null },
                onOpenDetail = { event ->
                    previewEvent = null
                    onOpenEventDetails(event.id)
                },
                onDismissList = { clusterEvents = null },
                onSelectFromList = { event ->
                    clusterEvents = null
                    previewEvent = event
                },
            )
        }
    }
}

/** Best-effort current camera bounds; falls back to the fixed default extent before the map has laid out. */
private fun visibleBounds(cameraPositionState: CameraPositionState): MapBounds? =
    cameraPositionState.projection?.visibleRegion?.latLngBounds?.let {
        MapBounds(
            minLat = it.southwest.latitude,
            maxLat = it.northeast.latitude,
            minLng = it.southwest.longitude,
            maxLng = it.northeast.longitude,
        )
    }
