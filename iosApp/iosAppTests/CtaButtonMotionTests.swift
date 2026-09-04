import XCTest
@testable import iosApp

final class CtaButtonMotionTests: XCTestCase {
    func test_GIVEN_theMapaCtaAtRest_WHEN_readingItsBackgroundAlpha_THEN_itIsTheRestingTint() {
        XCTAssertEqual(CtaButtonMotion.mapaBackgroundAlpha(pressed: false), CtaButtonMotion.mapaRestingAlpha)
    }

    func test_GIVEN_theMapaCtaPressed_WHEN_readingItsBackgroundAlpha_THEN_itIsFullySolid() {
        XCTAssertEqual(CtaButtonMotion.mapaBackgroundAlpha(pressed: true), 1)
    }

    func test_GIVEN_theInstagramCtaAtRest_WHEN_readingItsGradientOffset_THEN_itIsZero() {
        XCTAssertEqual(CtaButtonMotion.instagramGradientOffset(pressed: false), 0)
    }

    func test_GIVEN_theInstagramCtaPressed_WHEN_readingItsGradientOffset_THEN_itIsOne() {
        XCTAssertEqual(CtaButtonMotion.instagramGradientOffset(pressed: true), 1)
    }
}
