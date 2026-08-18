package com.qualorock.android.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.qualorock.shared.domain.Event

/** Wraps an [Event] so android-maps-utils can cluster it — the server already guarantees valid venue coordinates. */
class EventClusterItem(val event: Event) : ClusterItem {
    private val position =
        LatLng(
            requireNotNull(event.venue.latitude) { "map markers must have a venue with coordinates" },
            requireNotNull(event.venue.longitude) { "map markers must have a venue with coordinates" },
        )

    override fun getPosition(): LatLng = position

    override fun getTitle(): String = event.title

    override fun getSnippet(): String = event.venue.name

    override fun getZIndex(): Float = 0f
}
