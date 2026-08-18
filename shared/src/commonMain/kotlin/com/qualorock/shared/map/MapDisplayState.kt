package com.qualorock.shared.map

import com.qualorock.shared.domain.Event
import com.qualorock.shared.filters.FilterState

/** A fixed camera/viewport bounding box — never derived from device location (MAP-001/005). */
data class MapBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
) {
    fun contains(
        latitude: Double?,
        longitude: Double?,
    ): Boolean = latitude != null && longitude != null && latitude in minLat..maxLat && longitude in minLng..maxLng
}

sealed interface MapDisplayState {
    data class Markers(val events: List<Event>) : MapDisplayState

    /** Zero events visible — either the current viewport has none, or (with active filters) none exist at all. */
    data object EmptyViewport : MapDisplayState

    /** Zero events match the active filters anywhere, distinct from [EmptyViewport] per MAP-003/006. */
    data class NoFilterResults(val canClear: Boolean) : MapDisplayState
}

/**
 * The unit-testable equivalent of cluster-grouping logic (design.md) — actual marker clustering is delegated
 * to each platform's native map SDK (Android Maps Utils / MapKit clustering), never computed here. This only
 * derives which of the four MAP-006/011 display states applies, given fetched markers, the visible camera
 * bounds, and the currently active filters.
 */
object MapDisplayStateDeriver {
    fun derive(
        markers: List<Event>,
        visibleBounds: MapBounds,
        activeFilters: FilterState,
    ): MapDisplayState {
        if (markers.isEmpty()) {
            return if (!activeFilters.isEmpty) {
                MapDisplayState.NoFilterResults(canClear = true)
            } else {
                MapDisplayState.EmptyViewport
            }
        }
        val visible = markers.filter { visibleBounds.contains(it.venue.latitude, it.venue.longitude) }
        return if (visible.isEmpty()) MapDisplayState.EmptyViewport else MapDisplayState.Markers(visible)
    }
}
