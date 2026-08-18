import MapKit
import Shared

/// Wraps an `Event` so MapKit can place/cluster it — the server already guarantees valid venue coordinates
/// for events returned by `/api/events/map`, but the initializer still fails safely rather than defaulting
/// to (0, 0) ("Null Island") in case that invariant is ever violated.
final class EventAnnotation: NSObject, MKAnnotation {
    let event: Event
    let coordinate: CLLocationCoordinate2D

    init?(event: Event) {
        guard let latitude = event.venue.latitude?.doubleValue, let longitude = event.venue.longitude?.doubleValue else {
            return nil
        }
        self.event = event
        coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }

    var title: String? { event.title }
    var subtitle: String? { event.venue.name }
}
