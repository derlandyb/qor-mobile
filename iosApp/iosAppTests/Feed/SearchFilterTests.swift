@testable import iosApp
import Shared
import XCTest

final class SearchFilterTests: XCTestCase {
    /// The wrapper's `@Published` properties are updated from a Kotlin `Dispatchers.Main` coroutine,
    /// which — like `DispatchQueue.main.async` — needs a run-loop turn to deliver, even when the
    /// mutating call itself already ran synchronously on the main thread.
    private func pumpRunLoop() {
        RunLoop.current.run(until: Date().addingTimeInterval(0.1))
    }

    @MainActor
    private func makeWrapper() -> FeedQueryViewModelWrapper {
        let filterOwner = IosSharedFilterViewModel(baseUrl: "http://127.0.0.1:1")
        return FeedQueryViewModelWrapper(baseUrl: "http://127.0.0.1:1", filterViewModel: filterOwner.filterViewModel)
    }

    @MainActor
    func testGivenAWrapperWhenSetQueryIsCalledThenTheQueryIsPublished() {
        let wrapper = makeWrapper()

        wrapper.setQuery("forro")

        XCTAssertEqual(wrapper.query, "forro")
    }

    @MainActor
    func testGivenAWrapperWhenNoQueryOrFiltersAreActiveThenResultsStateIsInactive() {
        let wrapper = makeWrapper()

        XCTAssertTrue(wrapper.resultsState is FeedResultsUiStateInactive)
    }

    @MainActor
    func testGivenAWrapperWhenSelectCityIsCalledThenFilterStateReflectsIt() {
        let wrapper = makeWrapper()

        wrapper.selectCity(city: "Vila Velha")
        pumpRunLoop()

        XCTAssertEqual(wrapper.filterState.city, "Vila Velha")
    }

    @MainActor
    func testGivenActiveFiltersWhenClearAllIsCalledThenFilterStateResetsToEmpty() {
        let wrapper = makeWrapper()
        wrapper.selectCity(city: "Vila Velha")
        wrapper.toggleGenre(genre: "Rock")
        pumpRunLoop()
        XCTAssertFalse(wrapper.filterState.isEmpty)

        wrapper.clearAll()
        pumpRunLoop()

        XCTAssertTrue(wrapper.filterState.isEmpty)
    }

    func testGivenAnActiveQueryAndFilterWhenDescribeActiveQueryIsCalledThenBothAreNamed() {
        let filters = FilterState(dateBucket: nil, city: "Vila Velha", genres: [], artist: nil)

        let summary = describeActiveQuery(filters: filters, query: "forro")

        XCTAssertEqual(summary, "\"forro\" · Vila Velha")
    }

    func testGivenNoActiveQueryOrFiltersWhenDescribeActiveQueryIsCalledThenNilIsReturned() {
        let filters = FilterState(dateBucket: nil, city: nil, genres: [], artist: nil)

        let summary = describeActiveQuery(filters: filters, query: "")

        XCTAssertNil(summary)
    }
}
