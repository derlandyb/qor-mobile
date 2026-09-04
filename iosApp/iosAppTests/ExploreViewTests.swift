import XCTest
import ViewInspector
import shared
@testable import iosApp

/// I12 — render/state assertions for `ExploreView` via ViewInspector, per the I7-I14 track's test
/// tooling note (pure-logic coverage lives in `ExploreViewModelTests`).
@MainActor
final class ExploreViewTests: XCTestCase {
    func test_exploreScreen_isAccessibleForInspection() throws {
        let view = ExploreView(onEventClick: { _ in })
        XCTAssertNoThrow(try view.inspect())
    }

    func test_exploreScreen_hasStableAccessibilityIdentifier() throws {
        let view = ExploreView(onEventClick: { _ in })
        let root = try view.inspect().vStack()
        XCTAssertEqual(try root.accessibilityIdentifier(), "explore_screen")
    }
}
