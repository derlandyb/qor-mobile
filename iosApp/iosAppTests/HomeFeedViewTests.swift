import XCTest
import ViewInspector
import shared
@testable import iosApp

private final class FakeHomeFeedEventsGateway: HomeFeedEventsGateway {
    var pagesByCursor: [String?: EventPage] = [:]
    var errorToThrow: Error?

    func loadUpcoming(city: City?, genre: String?, cursor: String?) async throws -> EventPage {
        if let errorToThrow {
            throw errorToThrow
        }
        return pagesByCursor[cursor] ?? EventPage(events: [], nextCursor: nil)
    }
}

private final class FakeHomeFeedPollingGateway: HomeFeedPollingGateway {
    func start(city: City?, genre: String?) {}
    func stop() {}
    func refreshNow() async throws {}
    func currentPage() -> EventPage { EventPage(events: [], nextCursor: nil) }
    func observeUpdates(_ onUpdate: @escaping (EventPage) -> Void) {}
}

private enum StubError: Error {
    case failed
}

private func makeEvent(id: String = "1") -> Event {
    Event(
        id: id,
        title: "Show de Rock",
        description: "desc",
        coverImageUrl: nil,
        startsAt: "2026-10-01T20:00:00Z",
        city: .vitoria,
        genre: "rock",
        address: "Rua X, 100",
        isFree: false,
        ticketUrl: nil
    )
}

@MainActor
private func waitUntil(timeout: TimeInterval = 1.0, _ condition: () -> Bool) async {
    let deadline = Date().addingTimeInterval(timeout)
    while !condition(), Date() < deadline {
        try? await Task.sleep(nanoseconds: 10_000_000)
    }
}

@MainActor
final class HomeFeedViewTests: XCTestCase {
    func test_GIVEN_slowInitialLoad_WHEN_viewFirstRenders_THEN_loadingIndicatorIsShown() async throws {
        let eventsGateway = FakeHomeFeedEventsGateway()
        let pollingGateway = FakeHomeFeedPollingGateway()
        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        let view = HomeFeedView(onEventClick: { _ in }, viewModel: viewModel)

        let found = try view.inspect().find(viewWithAccessibilityIdentifier: "home_feed_loading")
        XCTAssertNotNil(found)
    }

    func test_GIVEN_emptyEventPage_WHEN_initialLoadCompletes_THEN_emptyStateIsShown() async throws {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(events: [], nextCursor: nil)
        let pollingGateway = FakeHomeFeedPollingGateway()
        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)

        await waitUntil { if case .empty = viewModel.uiState { return true }; return false }

        let view = HomeFeedView(onEventClick: { _ in }, viewModel: viewModel)
        let found = try view.inspect().find(viewWithAccessibilityIdentifier: "home_feed_empty")
        XCTAssertNotNil(found)
    }

    func test_GIVEN_failedInitialLoad_WHEN_stateBecomesError_THEN_errorMessageAndRetryButtonAreShown() async throws {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.errorToThrow = StubError.failed
        let pollingGateway = FakeHomeFeedPollingGateway()
        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)

        await waitUntil { if case .error = viewModel.uiState { return true }; return false }

        let view = HomeFeedView(onEventClick: { _ in }, viewModel: viewModel)
        let errorContainer = try view.inspect().find(viewWithAccessibilityIdentifier: "home_feed_error")
        XCTAssertNotNil(errorContainer)
        let retryButton = try view.inspect().find(viewWithAccessibilityIdentifier: "home_feed_retry")
        XCTAssertNotNil(retryButton)
    }

    func test_GIVEN_eventsLoaded_WHEN_stateBecomesContent_THEN_oneCardPerEventIsShown() async throws {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(
            events: [makeEvent(id: "1"), makeEvent(id: "2")],
            nextCursor: nil
        )
        let pollingGateway = FakeHomeFeedPollingGateway()
        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)

        await waitUntil { if case .content = viewModel.uiState { return true }; return false }

        let view = HomeFeedView(onEventClick: { _ in }, viewModel: viewModel)
        XCTAssertNotNil(try view.inspect().find(viewWithAccessibilityIdentifier: "home_feed_event_1"))
        XCTAssertNotNil(try view.inspect().find(viewWithAccessibilityIdentifier: "home_feed_event_2"))
    }

    func test_GIVEN_eventsLoaded_WHEN_tappingACard_THEN_onEventClickFiresWithThatEventId() async throws {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(events: [makeEvent(id: "42")], nextCursor: nil)
        let pollingGateway = FakeHomeFeedPollingGateway()
        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)

        await waitUntil { if case .content = viewModel.uiState { return true }; return false }

        var clickedId: String?
        let view = HomeFeedView(onEventClick: { id in clickedId = id }, viewModel: viewModel)

        let card = try view.inspect().find(viewWithAccessibilityIdentifier: "home_feed_event_42")
        try card.find(ViewType.Button.self).tap()

        XCTAssertEqual(clickedId, "42")
    }
}
