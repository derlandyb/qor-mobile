import XCTest
@testable import iosApp

/// Every user-facing string must render in Brazilian Portuguese unconditionally, per
/// `ARCHITECTURE.md` §9 — never falling back to English (or a raw `String(localized:)` key) just
/// because a device/simulator's system language isn't set to pt-BR. `Localizable.xcstrings` only
/// ever declares pt-BR translations, so the app's declared development region is what makes that
/// resolution unconditional rather than dependent on matching the system's preferred languages.
final class LocalizationTests: XCTestCase {
    func test_GIVEN_theAppBundle_WHEN_inspectingItsLocalization_THEN_theDevelopmentRegionIsPtBR() {
        XCTAssertEqual(Bundle.main.developmentLocalization, "pt-BR")
    }
}
