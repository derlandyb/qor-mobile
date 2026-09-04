import XCTest
import shared
@testable import iosApp

/// I12 — pure state-shape assertions for `ExploreUiState`/filter-toggle logic. `ExploreViewModel`
/// resolves `IosDependencies.shared` at `init`, which needs `di.doInitKoin()` to have run —
/// `iosAppTests` is hosted inside the `iosApp` application target, which already calls
/// `KoinHelperKt.doInitKoin()` from `IosAppApp`'s `init` before any test runs, so tests must NOT
/// call it again (Koin's `startKoin` throws `KoinAppAlreadyStartedException` on a second call,
/// which crashes the whole process — Kotlin/Native has no automatic bridge from an uncaught
/// Kotlin exception to a catchable Swift/XCTest failure).
@MainActor
final class ExploreViewModelTests: XCTestCase {

    func test_toggleCity_selectingSameCityTwice_clearsSelection() {
        let viewModel = ExploreViewModel()
        XCTAssertNil(viewModel.selectedCity)

        viewModel.onCitySelected(.vitoria)
        XCTAssertEqual(viewModel.selectedCity, .vitoria)

        viewModel.onCitySelected(.vitoria)
        XCTAssertNil(viewModel.selectedCity)
    }

    func test_toggleGenre_selectingDifferentGenre_replacesSelection() {
        let viewModel = ExploreViewModel()

        viewModel.onGenreSelected("Rock")
        XCTAssertEqual(viewModel.selectedGenre, "Rock")

        viewModel.onGenreSelected("Samba")
        XCTAssertEqual(viewModel.selectedGenre, "Samba")
    }

    func test_clearFilters_resetsBothCityAndGenre() {
        let viewModel = ExploreViewModel()
        viewModel.onCitySelected(.serra)
        viewModel.onGenreSelected("Reggae")

        viewModel.onClearFilters()

        XCTAssertNil(viewModel.selectedCity)
        XCTAssertNil(viewModel.selectedGenre)
    }

    func test_bothFilters_areAndCombined_neitherDiscardsTheOther() {
        let viewModel = ExploreViewModel()
        viewModel.onCitySelected(.cariacica)
        viewModel.onGenreSelected("Eletrônico")

        XCTAssertEqual(viewModel.selectedCity, .cariacica)
        XCTAssertEqual(viewModel.selectedGenre, "Eletrônico")
    }

    func test_uiState_startsLoading() {
        let viewModel = ExploreViewModel()
        XCTAssertEqual(viewModel.uiState, .loading)
    }
}
