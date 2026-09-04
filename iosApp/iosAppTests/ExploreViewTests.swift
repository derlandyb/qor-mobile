import XCTest
import ViewInspector
import shared
@testable import iosApp

private final class NoopEventsGateway: HomeFeedEventsGateway {
    func loadUpcoming(city: City?, genre: String?, cursor: String?) async throws -> EventPage {
        EventPage(events: [], nextCursor: nil)
    }
}

private final class NoopPollingGateway: HomeFeedPollingGateway {
    func start(city: City?, genre: String?) {}
    func stop() {}
    func refreshNow() async throws {}
    func currentPage() -> EventPage { EventPage(events: [], nextCursor: nil) }
    func observeUpdates(_ onUpdate: @escaping (EventPage) -> Void) {}
}

/// I12 — render/state assertions for `ExploreView` via ViewInspector, per the I7-I14 track's test
/// tooling note (pure-logic coverage lives in `ExploreViewModelTests`). Injects a fake-backed
/// view model, same reasoning as `ExploreViewModelTests`.
@MainActor
final class ExploreViewTests: XCTestCase {
    private func makeView() -> ExploreView {
        let viewModel = ExploreViewModel(eventsGateway: NoopEventsGateway(), pollingGateway: NoopPollingGateway())
        return ExploreView(onEventClick: { _ in }, viewModel: viewModel)
    }

    func test_exploreScreen_isAccessibleForInspection() throws {
        XCTAssertNoThrow(try makeView().inspect())
    }

    func test_exploreScreen_hasStableAccessibilityIdentifier() throws {
        let root = try makeView().inspect().vStack()
        XCTAssertEqual(try root.accessibilityIdentifier(), "explore_screen")
    }
}
