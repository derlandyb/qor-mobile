import XCTest
@testable import iosApp

final class GenreTagColorsTests: XCTestCase {
    func test_GIVEN_sertanejo_WHEN_resolvingItsStyle_THEN_itUsesTheSolidTreatment() {
        let style = GenreTagColors.style(for: "Sertanejo")

        XCTAssertEqual(style.backgroundColor, QorColor.accentPink)
        XCTAssertEqual(style.textColor, QorColor.bgBase)
    }

    func test_GIVEN_anUnknownGenre_WHEN_resolvingItsStyle_THEN_itFallsBackToANeutralTint() {
        let style = GenreTagColors.style(for: "Forró")

        XCTAssertEqual(style.backgroundColor, QorColor.surfaceCardHover)
        XCTAssertEqual(style.textColor, QorColor.textSecondary)
    }
}
