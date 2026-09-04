import XCTest
import shared
@testable import iosApp

private final class FakeExploreEventsGateway: HomeFeedEventsGateway {
    func loadUpcoming(city: City?, genre: String?, cursor: String?) async throws -> EventPage {
        EventPage(events: [], nextCursor: nil)
    }
}

private final class FakeExplorePollingGateway: HomeFeedPollingGateway {
    func start(city: City?, genre: String?) {}
    func stop() {}
    func refreshNow() async throws {}
    func currentPage() -> EventPage { EventPage(events: [], nextCursor: nil) }
    func observeUpdates(_ onUpdate: @escaping (EventPage) -> Void) {}
}

/// I12 — pure state-shape assertions for `ExploreUiState`/filter-toggle logic. Injects fakes for
/// both gateways so these tests never touch the real Koin graph or make a real network call —
/// `ExploreViewModel`'s own doc comment explains why that matters beyond just these tests.
@MainActor
final class ExploreViewModelTests: XCTestCase {
    private func makeViewModel() -> ExploreViewModel {
        ExploreViewModel(eventsGateway: FakeExploreEventsGateway(), pollingGateway: FakeExplorePollingGateway())
    }

    func test_toggleCity_selectingSameCityTwice_clearsSelection() {
        let viewModel = makeViewModel()
        XCTAssertNil(viewModel.selectedCity)

        viewModel.onCitySelected(.vitoria)
        XCTAssertEqual(viewModel.selectedCity, .vitoria)

        viewModel.onCitySelected(.vitoria)
        XCTAssertNil(viewModel.selectedCity)
    }

    func test_toggleGenre_selectingDifferentGenre_replacesSelection() {
        let viewModel = makeViewModel()

        viewModel.onGenreSelected("Rock")
        XCTAssertEqual(viewModel.selectedGenre, "Rock")

        viewModel.onGenreSelected("Samba")
        XCTAssertEqual(viewModel.selectedGenre, "Samba")
    }

    func test_clearFilters_resetsBothCityAndGenre() {
        let viewModel = makeViewModel()
        viewModel.onCitySelected(.serra)
        viewModel.onGenreSelected("Reggae")

        viewModel.onClearFilters()

        XCTAssertNil(viewModel.selectedCity)
        XCTAssertNil(viewModel.selectedGenre)
    }

    func test_bothFilters_areAndCombined_neitherDiscardsTheOther() {
        let viewModel = makeViewModel()
        viewModel.onCitySelected(.cariacica)
        viewModel.onGenreSelected("Eletrônico")

        XCTAssertEqual(viewModel.selectedCity, .cariacica)
        XCTAssertEqual(viewModel.selectedGenre, "Eletrônico")
    }

    func test_uiState_startsLoading() {
        let viewModel = makeViewModel()
        XCTAssertEqual(viewModel.uiState, .loading)
    }
}
