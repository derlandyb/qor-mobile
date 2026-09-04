import XCTest
import SwiftUI
import ViewInspector
import shared
@testable import iosApp

/// Renders `EventDetailView` off an already-loaded `EventDetailViewModel` (built via
/// `await viewModel.load(...)` before construction) and inspects it un-hosted. `.task` never
/// fires on an un-hosted view, so the viewModel's already-published state is what `body`
/// resolves against — no async-timing race with `.task` re-triggering a fresh load.
@MainActor
final class EventDetailViewTests: XCTestCase {
    private func makeEvent() -> Event {
        Event(
            id: "evt-1",
            title: "Rock na Praça",
            description: "Uma noite de rock ao vivo.",
            coverImageUrl: nil,
            startsAt: "2026-10-01T22:00:00Z",
            city: City.vitoria,
            genre: "Rock",
            address: "Rua Barão de Monjardim, 100",
            isFree: true,
            ticketUrl: nil
        )
    }

    func test_GIVEN_activeEventWithMap_WHEN_rendered_THEN_showsTitleAndMap() async throws {
        let event = makeEvent()
        let point = GeoPoint(latitude: -20.32, longitude: -40.34)
        let viewModel = EventDetailViewModel(
            fetchDetail: { _ in EventDetail.Active(event: event, promoters: []) },
            geocode: { _ in [point] }
        )
        await viewModel.load(eventId: event.id)
        let view = EventDetailView(eventId: event.id, viewModel: viewModel)

        let title = try view.inspect().find(viewWithAccessibilityIdentifier: "event_detail_title").text().string()
        XCTAssertEqual(title, event.title)
        XCTAssertNoThrow(try view.inspect().find(viewWithAccessibilityIdentifier: "event_detail_map"))
    }

    func test_GIVEN_geocodeFails_WHEN_rendered_THEN_showsMapFallbackLink() async throws {
        let event = makeEvent()
        let viewModel = EventDetailViewModel(
            fetchDetail: { _ in EventDetail.Active(event: event, promoters: []) },
            geocode: { _ in nil }
        )
        await viewModel.load(eventId: event.id)
        let view = EventDetailView(eventId: event.id, viewModel: viewModel)

        XCTAssertNoThrow(try view.inspect().find(viewWithAccessibilityIdentifier: "event_detail_map_fallback"))
        XCTAssertThrowsError(try view.inspect().find(viewWithAccessibilityIdentifier: "event_detail_map"))
    }

    func test_GIVEN_cancelledEvent_WHEN_rendered_THEN_noTicketButton() async throws {
        let event = makeEvent()
        let viewModel = EventDetailViewModel(
            fetchDetail: { _ in EventDetail.Cancelled(event: event, promoters: []) },
            geocode: { _ in nil }
        )
        await viewModel.load(eventId: event.id)
        let view = EventDetailView(eventId: event.id, viewModel: viewModel)

        XCTAssertThrowsError(try view.inspect().find(viewWithAccessibilityIdentifier: "event_detail_ticket_button"))
    }

    func test_GIVEN_loadFails_WHEN_rendered_THEN_showsRetryButton() async throws {
        struct Boom: Error {}
        let viewModel = EventDetailViewModel(
            fetchDetail: { _ in throw Boom() },
            geocode: { _ in nil }
        )
        await viewModel.load(eventId: "evt-1")
        let view = EventDetailView(eventId: "evt-1", viewModel: viewModel)

        let retryText = try view.inspect().find(text: String(localized: "event_detail_cta_tentar_novamente"))
        XCTAssertNoThrow(retryText)
    }
}
