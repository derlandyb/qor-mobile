import MapKit
import Shared
import SwiftUI

private let eventReuseIdentifier = "event"
private let eventClusteringIdentifier = "event-cluster"

/// `UIViewRepresentable` wrapping `MKMapView` — chosen over SwiftUI's native `Map` specifically to reach
/// `MKMarkerAnnotationView.clusteringIdentifier`/`MKClusterAnnotation`, which SwiftUI's declarative `Map`
/// doesn't expose. Never derives its region from device location (MAP-001/005) — always the fixed
/// Grande Vitória extent passed in by the caller.
struct MapViewRepresentable: UIViewRepresentable {
    let events: [Event]
    let region: MKCoordinateRegion
    let onSelectSingle: (Event) -> Void
    let onSelectCluster: ([Event]) -> Void

    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView()
        mapView.delegate = context.coordinator
        mapView.setRegion(region, animated: false)
        mapView.pointOfInterestFilter = .excludingAll
        return mapView
    }

    func updateUIView(_ mapView: MKMapView, context: Context) {
        let existing = mapView.annotations.compactMap { $0 as? EventAnnotation }
        let existingIds = Set(existing.map(\.event.id))
        let newIds = Set(events.map(\.id))
        guard existingIds != newIds else { return }
        mapView.removeAnnotations(existing)
        mapView.addAnnotations(events.map { EventAnnotation(event: $0) })
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onSelectSingle: onSelectSingle, onSelectCluster: onSelectCluster)
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        private let onSelectSingle: (Event) -> Void
        private let onSelectCluster: ([Event]) -> Void

        init(onSelectSingle: @escaping (Event) -> Void, onSelectCluster: @escaping ([Event]) -> Void) {
            self.onSelectSingle = onSelectSingle
            self.onSelectCluster = onSelectCluster
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            guard let eventAnnotation = annotation as? EventAnnotation else { return nil }
            let view =
                mapView.dequeueReusableAnnotationView(withIdentifier: eventReuseIdentifier) as? MKMarkerAnnotationView
                    ?? MKMarkerAnnotationView(annotation: eventAnnotation, reuseIdentifier: eventReuseIdentifier)
            view.annotation = eventAnnotation
            view.clusteringIdentifier = eventClusteringIdentifier
            view.canShowCallout = false
            return view
        }

        /// Distinguishes a plain marker (MAP-002/009: opens a single-event preview) from an
        /// `MKClusterAnnotation` — either a same-venue multi-event marker or a geographic cluster both
        /// resolve to the same list-on-tap (MAP-004/010).
        func mapView(_ mapView: MKMapView, didSelect annotation: MKAnnotation) {
            defer { mapView.deselectAnnotation(annotation, animated: false) }
            if let cluster = annotation as? MKClusterAnnotation {
                onSelectCluster(cluster.memberAnnotations.compactMap { ($0 as? EventAnnotation)?.event })
            } else if let eventAnnotation = annotation as? EventAnnotation {
                onSelectSingle(eventAnnotation.event)
            }
        }
    }
}
