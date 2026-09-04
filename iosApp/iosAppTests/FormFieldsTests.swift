import XCTest
@testable import iosApp

final class FormFieldsTests: XCTestCase {
    func test_GIVEN_pastedTextWithNonDigits_WHEN_sanitizingOtpInput_THEN_onlyDigitsRemain() {
        XCTAssertEqual(sanitizeOtpInput("1a2b3c"), "123")
    }

    func test_GIVEN_moreThanSixDigits_WHEN_sanitizingOtpInput_THEN_itIsCappedAtSix() {
        XCTAssertEqual(sanitizeOtpInput("1234567890"), "123456")
    }
}
