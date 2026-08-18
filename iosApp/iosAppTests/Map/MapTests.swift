@testable import iosApp
import MapKit
import Shared
import XCTest

final class MapTests: XCTestCase {
    private func event(id: String) -> Event {
        let utc = Kotlinx_datetimeTimeZone.Companion.shared.UTC
        let dateTime = Kotlinx_datetimeLocalDateTime(
            year: 2026, monthNumber: 8, dayOfMonth: 16, hour: 22, minute: 0, second: 0, nanosecond: 0
        )
        return Event(
            id: id,
            title: "Show \(id)",
            description: nil,
            coverImageUrl: nil,
            startDateTime: utc.toInstant(dateTime),
            endDateTime: nil,
            venue: Venue(
                id: "v-\(id)",
                name: "Venue \(id)",
                imageUrl: nil,
                description: nil,
                city: "Vitória",
                address: nil,
                latitude: -20.31,
                longitude: -40.31,
                staticMapUrl: nil,
                contactPhone: nil,
                contactEmail: nil,
                socialLinks: nil,
                verificationStatus: VerificationStatus.verified
            ),
            city: "Vitória",
            price: nil,
            ageRating: nil,
            genres: [],
            ticketUrl: nil,
            status: EventStatus.published,
            bannerStatus: nil,
            promoter: nil,
            isFavorited: nil
        )
    }

    func testGivenAVisitorWhenSelectingAMarkerThenItsEventPreviewOpens() {
        var opened: Event?
        let coordinator = MapViewRepresentable.Coordinator(
            onSelectSingle: { opened = $0 },
            onSelectCluster: { _ in }
        )
        let mapView = MKMapView()
        let annotation = EventAnnotation(event: event(id: "1"))

        coordinator.mapView(mapView, didSelect: annotation)

        XCTAssertEqual(opened?.id, "1")
    }

    func testGivenAVisitorWhenSelectingAClusterMarkerThenEveryConstituentEventIsListed() {
        var listed: [Event] = []
        let coordinator = MapViewRepresentable.Coordinator(
            onSelectSingle: { _ in },
            onSelectCluster: { listed = $0 }
        )
        let mapView = MKMapView()
        let cluster = MKClusterAnnotation(memberAnnotations: [
            EventAnnotation(event: event(id: "1")),
            EventAnnotation(event: event(id: "2"))
        ])

        coordinator.mapView(mapView, didSelect: cluster)

        XCTAssertEqual(Set(listed.map(\.id)), Set(["1", "2"]))
    }

    func testGivenAnEventWithCoordinatesWhenWrappedThenTheAnnotationCoordinateMatchesTheVenue() {
        let annotation = EventAnnotation(event: event(id: "1"))

        XCTAssertEqual(annotation.coordinate.latitude, -20.31, accuracy: 0.0001)
        XCTAssertEqual(annotation.coordinate.longitude, -40.31, accuracy: 0.0001)
    }

    @MainActor
    func testGivenAWrapperWhenObservedThenInitialMarkersStateIsLoading() {
        let filterOwner = IosSharedFilterViewModel(baseUrl: "http://127.0.0.1:1")
        let wrapper = MapQueryViewModelWrapper(baseUrl: "http://127.0.0.1:1", filterViewModel: filterOwner.filterViewModel)

        XCTAssertTrue(wrapper.markersState is MapMarkersUiStateLoading)
        wrapper.retry()
    }
}
