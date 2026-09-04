import XCTest
@testable import iosApp

final class EmptyStateTests: XCTestCase {
    func test_GIVEN_noMessageOverride_WHEN_creatingEmptyState_THEN_itDefaultsToTheLocalizedNoEventsCopy() {
        let state = EmptyState()

        XCTAssertEqual(state.message, String(localized: "empty_state_no_events"))
    }
}
