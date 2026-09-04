import XCTest
@testable import iosApp

final class EntranceStaggerTests: XCTestCase {
    func test_GIVEN_theFirstGridItem_WHEN_readingItsStaggerDelay_THEN_itHasNoDelay() {
        XCTAssertEqual(entranceStaggerDelaySeconds(index: 0), 0)
    }

    func test_GIVEN_theThirdGridItem_WHEN_readingItsStaggerDelay_THEN_itDelaysByTwoStaggerIncrements() {
        XCTAssertEqual(entranceStaggerDelaySeconds(index: 2), QorMotion.durationStagger * 2, accuracy: 0.0001)
    }
}
