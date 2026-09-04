import XCTest
@testable import iosApp

final class BottomNavDestinationTests: XCTestCase {
    func test_GIVEN_favoritos_WHEN_readingEnabled_THEN_itIsDisabledPerMilestone2Scope() {
        XCTAssertFalse(BottomNavDestination.favoritos.enabled)
    }

    func test_GIVEN_everyOtherDestination_WHEN_readingEnabled_THEN_theyAreEnabled() {
        for destination in BottomNavDestination.allCases where destination != .favoritos {
            XCTAssertTrue(destination.enabled, "\(destination) should be enabled")
        }
    }

    func test_GIVEN_allDestinations_WHEN_readingTheirLabelKeys_THEN_eachIsDistinct() {
        let keys = BottomNavDestination.allCases.map { String(describing: $0.labelKey) }

        XCTAssertEqual(Set(keys).count, BottomNavDestination.allCases.count)
    }
}
