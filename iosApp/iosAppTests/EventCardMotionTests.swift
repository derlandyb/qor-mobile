import XCTest
@testable import iosApp

final class EventCardMotionTests: XCTestCase {
    func test_GIVEN_theCardAtRest_WHEN_readingItsScale_THEN_itIsOne() {
        XCTAssertEqual(EventCardMotion.pressScale(pressed: false), 1)
    }

    func test_GIVEN_theCardPressed_WHEN_readingItsScaleAndRise_THEN_itScalesUpAndRisesPerDesignSystem() {
        XCTAssertEqual(EventCardMotion.pressScale(pressed: true), 1.03)
        XCTAssertEqual(EventCardMotion.pressRiseY(pressed: true), -4)
    }
}
