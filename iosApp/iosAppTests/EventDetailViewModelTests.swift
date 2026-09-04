import XCTest
import shared
@testable import iosApp

@MainActor
final class EventDetailViewModelTests: XCTestCase {
    private func makeEvent(address: String = "Rua Barão de Monjardim, 100") -> Event {
        Event(
            id: "evt-1",
            title: "Rock na Praça",
            description: "Uma noite de rock ao vivo.",
            coverImageUrl: nil,
            startsAt: "2026-10-01T22:00:00Z",
            city: City.vitoria,
            genre: "Rock",
            address: address,
            isFree: true,
            ticketUrl: nil
        )
    }

    func test_GIVEN_activeEvent_WHEN_loadSucceeds_THEN_contentStateAndGeocodedMap() async {
        let event = makeEvent()
        let point = GeoPoint(latitude: -20.32, longitude: -40.34)
        let viewModel = EventDetailViewModel(
            fetchDetail: { _ in EventDetail.Active(event: event, promoters: []) },
            geocode: { _ in [point] }
        )

        await viewModel.load(eventId: event.id)

        guard case .content(let detail) = viewModel.loadState else {
            return XCTFail("expected .content, got \(viewModel.loadState)")
        }
        XCTAssertTrue(detail is EventDetail.Active)
        XCTAssertEqual(viewModel.mapState, .located(point))
    }

    func test_GIVEN_cancelledEvent_WHEN_loadSucceeds_THEN_noGeocodeAttempted() async {
        let event = makeEvent()
        var geocodeCalled = false
        let viewModel = EventDetailViewModel(
            fetchDetail: { _ in EventDetail.Cancelled(event: event, promoters: []) },
            geocode: { _ in
                geocodeCalled = true
                return nil
            }
        )

        await viewModel.load(eventId: event.id)

        guard case .content(let detail) = viewModel.loadState else {
            return XCTFail("expected .content, got \(viewModel.loadState)")
        }
        XCTAssertTrue(detail is EventDetail.Cancelled)
        XCTAssertFalse(geocodeCalled)
        XCTAssertEqual(viewModel.mapState, .loading)
    }

    func test_GIVEN_geocodeFails_WHEN_load_THEN_mapStateFailed() async {
        let event = makeEvent()
        let viewModel = EventDetailViewModel(
            fetchDetail: { _ in EventDetail.Active(event: event, promoters: []) },
            geocode: { _ in nil }
        )

        await viewModel.load(eventId: event.id)

        XCTAssertEqual(viewModel.mapState, .failed)
    }

    func test_GIVEN_fetchDetailThrows_WHEN_load_THEN_errorState() async {
        struct Boom: Error {}
        let viewModel = EventDetailViewModel(
            fetchDetail: { _ in throw Boom() },
            geocode: { _ in nil }
        )

        await viewModel.load(eventId: "evt-1")

        XCTAssertEqual(viewModel.loadState, .error)
    }

    func test_toEventMapState_GIVEN_nilPoints_THEN_failed() {
        XCTAssertEqual(toEventMapState(nil), .failed)
    }

    func test_toEventMapState_GIVEN_emptyPoints_THEN_failed() {
        XCTAssertEqual(toEventMapState([]), .failed)
    }

    func test_toEventMapState_GIVEN_points_THEN_locatedWithFirst() {
        let first = GeoPoint(latitude: 1, longitude: 2)
        let second = GeoPoint(latitude: 3, longitude: 4)
        XCTAssertEqual(toEventMapState([first, second]), .located(first))
    }
}
