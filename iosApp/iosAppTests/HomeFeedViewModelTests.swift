import XCTest
import shared
@testable import iosApp

struct LoadCall: Equatable { let city: City?; let genre: String?; let cursor: String? }

private final class FakeHomeFeedEventsGateway: HomeFeedEventsGateway {
    var pagesByCursor: [String?: EventPage] = [:]
    var errorToThrow: Error?
    private(set) var loadCalls: [LoadCall] = []

    func loadUpcoming(city: City?, genre: String?, cursor: String?) async throws -> EventPage {
        loadCalls.append(LoadCall(city: city, genre: genre, cursor: cursor))
        if let errorToThrow {
            throw errorToThrow
        }
        return pagesByCursor[cursor] ?? EventPage(events: [], nextCursor: nil)
    }
}

private final class FakeHomeFeedPollingGateway: HomeFeedPollingGateway {
    private(set) var startCalls: [(city: City?, genre: String?)] = []
    private(set) var stopCallCount = 0
    var refreshError: Error?
    var refreshedPage = EventPage(events: [], nextCursor: nil)
    private var onUpdate: ((EventPage) -> Void)?

    func start(city: City?, genre: String?) {
        startCalls.append((city, genre))
    }

    func stop() {
        stopCallCount += 1
    }

    func refreshNow() async throws {
        if let refreshError {
            throw refreshError
        }
    }

    func currentPage() -> EventPage {
        refreshedPage
    }

    func observeUpdates(_ onUpdate: @escaping (EventPage) -> Void) {
        self.onUpdate = onUpdate
    }

    func emit(_ page: EventPage) {
        onUpdate?(page)
    }
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
final class HomeFeedViewModelTests: XCTestCase {
    func test_GIVEN_freshViewModel_WHEN_initialLoadSucceedsWithEvents_THEN_stateBecomesContent() async {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(events: [makeEvent()], nextCursor: "next")
        let pollingGateway = FakeHomeFeedPollingGateway()

        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        await waitUntil { if case .content = viewModel.uiState { return true }; return false }

        guard case .content(let events, let isLoadingMore, let isRefreshing) = viewModel.uiState else {
            return XCTFail("expected .content, got \(viewModel.uiState)")
        }
        XCTAssertEqual(events.map(\.id), ["1"])
        XCTAssertFalse(isLoadingMore)
        XCTAssertFalse(isRefreshing)
    }

    func test_GIVEN_freshViewModel_WHEN_initialLoadSucceedsWithNoEvents_THEN_stateBecomesEmpty() async {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(events: [], nextCursor: nil)
        let pollingGateway = FakeHomeFeedPollingGateway()

        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        await waitUntil { if case .empty = viewModel.uiState { return true }; return false }

        if case .empty = viewModel.uiState {
            // expected
        } else {
            XCTFail("expected .empty, got \(viewModel.uiState)")
        }
    }

    func test_GIVEN_freshViewModel_WHEN_initialLoadFails_THEN_stateBecomesError() async {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.errorToThrow = StubError.failed
        let pollingGateway = FakeHomeFeedPollingGateway()

        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        await waitUntil { if case .error = viewModel.uiState { return true }; return false }

        if case .error = viewModel.uiState {
            // expected
        } else {
            XCTFail("expected .error, got \(viewModel.uiState)")
        }
    }

    func test_GIVEN_contentWithNextCursor_WHEN_onLoadMoreCalled_THEN_pagesAreMerged() async {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(events: [makeEvent(id: "1")], nextCursor: "next")
        eventsGateway.pagesByCursor["next"] = EventPage(events: [makeEvent(id: "2")], nextCursor: nil)
        let pollingGateway = FakeHomeFeedPollingGateway()

        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        await waitUntil { if case .content = viewModel.uiState { return true }; return false }

        viewModel.onLoadMore()
        await waitUntil {
            if case .content(let events, _, _) = viewModel.uiState { return events.count == 2 }
            return false
        }

        guard case .content(let events, _, _) = viewModel.uiState else {
            return XCTFail("expected .content")
        }
        XCTAssertEqual(events.map(\.id), ["1", "2"])
    }

    func test_GIVEN_contentWithNoNextCursor_WHEN_onLoadMoreCalled_THEN_noAdditionalFetchHappens() async {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(events: [makeEvent()], nextCursor: nil)
        let pollingGateway = FakeHomeFeedPollingGateway()

        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        await waitUntil { if case .content = viewModel.uiState { return true }; return false }

        viewModel.onLoadMore()
        try? await Task.sleep(nanoseconds: 50_000_000)

        XCTAssertEqual(eventsGateway.loadCalls.count, 1)
    }

    func test_GIVEN_content_WHEN_refreshSucceeds_THEN_stateReflectsRefreshedPage() async {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(events: [makeEvent(id: "1")], nextCursor: nil)
        let pollingGateway = FakeHomeFeedPollingGateway()
        pollingGateway.refreshedPage = EventPage(events: [makeEvent(id: "1"), makeEvent(id: "2")], nextCursor: nil)

        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        await waitUntil { if case .content = viewModel.uiState { return true }; return false }

        await viewModel.refresh()

        guard case .content(let events, _, let isRefreshing) = viewModel.uiState else {
            return XCTFail("expected .content")
        }
        XCTAssertEqual(events.map(\.id), ["1", "2"])
        XCTAssertFalse(isRefreshing)
    }

    func test_GIVEN_content_WHEN_refreshFails_THEN_isRefreshingResetsWithoutLosingEvents() async {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(events: [makeEvent(id: "1")], nextCursor: nil)
        let pollingGateway = FakeHomeFeedPollingGateway()

        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        await waitUntil { if case .content = viewModel.uiState { return true }; return false }

        pollingGateway.refreshError = StubError.failed
        await viewModel.refresh()

        guard case .content(let events, _, let isRefreshing) = viewModel.uiState else {
            return XCTFail("expected .content to survive a failed refresh")
        }
        XCTAssertEqual(events.map(\.id), ["1"])
        XCTAssertFalse(isRefreshing)
    }

    func test_GIVEN_freshViewModel_WHEN_initialized_THEN_pollingGatewayIsStartedWithNoFilters() async {
        let eventsGateway = FakeHomeFeedEventsGateway()
        let pollingGateway = FakeHomeFeedPollingGateway()

        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        _ = viewModel

        XCTAssertEqual(pollingGateway.startCalls.count, 1)
        XCTAssertNil(pollingGateway.startCalls.first?.city)
    }

    func test_GIVEN_activeViewModel_WHEN_pollingGatewayEmitsANewPage_THEN_stateUpdatesToThatPage() async {
        let eventsGateway = FakeHomeFeedEventsGateway()
        eventsGateway.pagesByCursor[nil] = EventPage(events: [makeEvent(id: "1")], nextCursor: nil)
        let pollingGateway = FakeHomeFeedPollingGateway()

        let viewModel = HomeFeedViewModel(eventsGateway: eventsGateway, pollingGateway: pollingGateway)
        await waitUntil { if case .content = viewModel.uiState { return true }; return false }

        pollingGateway.emit(EventPage(events: [makeEvent(id: "1"), makeEvent(id: "9")], nextCursor: nil))
        await waitUntil {
            if case .content(let events, _, _) = viewModel.uiState { return events.count == 2 }
            return false
        }

        guard case .content(let events, _, _) = viewModel.uiState else {
            return XCTFail("expected .content")
        }
        XCTAssertEqual(events.map(\.id), ["1", "9"])
    }
}

@MainActor
private func waitUntil(
    timeout: TimeInterval = 1.0,
    _ condition: () -> Bool
) async {
    let deadline = Date().addingTimeInterval(timeout)
    while !condition(), Date() < deadline {
        try? await Task.sleep(nanoseconds: 10_000_000)
    }
}
