import XCTest
import shared
@testable import iosApp

final class CityFilterColorsTests: XCTestCase {
    func test_GIVEN_cariacica_WHEN_resolvingItsStyle_THEN_activeTextColorIsWhitePerDesignSystem() {
        let style = CityFilterColors.style(for: .cariacica)

        XCTAssertEqual(style.labelKey, "city_cariacica")
    }

    func test_GIVEN_everyCity_WHEN_resolvingItsStyle_THEN_eachHasADistinctLabelKey() {
        let keys = City.entries.map { CityFilterColors.style(for: $0).labelKey }

        XCTAssertEqual(Set(keys.map { String(describing: $0) }).count, City.entries.count)
    }
}
