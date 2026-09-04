import XCTest
@testable import iosApp

final class EventDateBadgeTests: XCTestCase {
    func test_GIVEN_anIsoTimestamp_WHEN_formattingTheDateBadge_THEN_itReturnsPtBrMonthAndDay() {
        let result = formatDateBadge(isoStartsAt: "2026-03-15T21:00:00Z")

        XCTAssertEqual(result.month, "MAR")
        XCTAssertEqual(result.day, "15")
    }

    func test_GIVEN_anIsoTimestamp_WHEN_formattingTheEventTime_THEN_itReturnsHhMm24h() {
        let result = formatEventTime(isoStartsAt: "2026-03-15T21:05:30Z")

        XCTAssertEqual(result, "21:05")
    }
}
