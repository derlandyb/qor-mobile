import Foundation

/// I13 — minimal WGS84 point, decoupled from `CLPlacemark`/`CLLocation` so [toEventMapState] (the
/// geocoding-result-to-map-state mapping) is plain-Swift unit-testable without a live `CLGeocoder`
/// call. Mirrors Android's `GeoPoint` (A22's `EventMapState.kt`).
struct GeoPoint: Equatable {
    let latitude: Double
    let longitude: Double
}

/// I13 — inline-map render state for `EventDetailView`'s embedded MapKit `Map`. Geocoding
/// `Event.address` client-side (`CLGeocoder`, mirroring Android's A22 retrofit and
/// `qor-website`'s `GoogleMap.tsx`) is a live network call that can return zero results or throw
/// — `.failed` drives the "Abrir no mapa" fallback link instead of a blank/broken map, the same
/// graceful-degradation contract Android's `EventMapState.Failed` and the website component use.
enum EventMapState: Equatable {
    case loading
    case located(GeoPoint)
    case failed
}

/// Pure mapping from a raw geocoding result (`nil` on failure/exception, empty when the address
/// had no match, first entry used when present — mirrors Android's `toEventMapState`/
/// `qor-website`'s `results?.[0]` check) to [EventMapState]. This is the only unit-testable slice
/// of I13's geocoding flow; the geocode call itself is a live network operation performed by
/// `EventDetailViewModel`.
func toEventMapState(_ points: [GeoPoint]?) -> EventMapState {
    guard let first = points?.first else { return .failed }
    return .located(first)
}
