import MapKit
import Shared

/// Wraps an `Event` so MapKit can place/cluster it — the server already guarantees valid venue coordinates.
final class EventAnnotation: NSObject, MKAnnotation {
    let event: Event
    let coordinate: CLLocationCoordinate2D

    init(event: Event) {
        self.event = event
        coordinate = CLLocationCoordinate2D(
            latitude: event.venue.latitude?.doubleValue ?? 0,
            longitude: event.venue.longitude?.doubleValue ?? 0
        )
    }

    var title: String? { event.title }
    var subtitle: String? { event.venue.name }
}
