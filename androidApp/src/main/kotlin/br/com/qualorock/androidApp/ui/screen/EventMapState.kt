package br.com.qualorock.androidApp.ui.screen

/**
 * A22 — minimal WGS84 point, decoupled from `android.location.Address` so [toEventMapState] (the
 * geocoding-result-to-map-state mapping) is plain-Kotlin unit-testable without Robolectric.
 */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * A22 — inline-map render state for `ActiveEventContent`'s embedded `GoogleMap`. Geocoding
 * `Event.address` client-side (Android `Geocoder`, mirroring `qor-website`'s `GoogleMap.tsx`) is
 * a live network call that can return zero results or throw — [Failed] drives the "abrir no
 * mapa" text-link fallback instead of a blank/crashed map, the same graceful-degradation
 * contract the website component uses.
 */
sealed interface EventMapState {
    data object Loading : EventMapState
    data class Located(val point: GeoPoint) : EventMapState
    data object Failed : EventMapState
}

/**
 * Pure mapping from a raw geocoding result (`null` on failure/exception, empty when the address
 * had no match, first entry used when present — mirrors `qor-website`'s `results?.[0]` check) to
 * [EventMapState]. This is the only unit-testable slice of A22's geocoding flow; the geocode call
 * itself is a live network operation performed in `ActiveEventContent`'s `LaunchedEffect`.
 */
fun toEventMapState(addresses: List<GeoPoint>?): EventMapState =
    addresses?.firstOrNull()?.let { EventMapState.Located(it) } ?: EventMapState.Failed
