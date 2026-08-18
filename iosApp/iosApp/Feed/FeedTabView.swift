import Shared
import SwiftUI

/// Owns the Feed tab's `@StateObject` wrappers and its own navigation state, so neither is torn down and
/// recreated by unrelated state changes elsewhere in the view tree (e.g. the Map tab's own navigation) —
/// see `MapTabView` for the matching Map-side isolation and the bug this pattern fixes.
struct FeedTabView: View {
    let filterViewModel: FilterViewModel

    @StateObject private var viewModel: EventFeedViewModelWrapper
    @StateObject private var queryViewModel: FeedQueryViewModelWrapper

    init(filterViewModel: FilterViewModel) {
        self.filterViewModel = filterViewModel
        _viewModel = StateObject(wrappedValue: EventFeedViewModelWrapper(baseUrl: AppConfig.apiBaseUrl))
        _queryViewModel = StateObject(
            wrappedValue: FeedQueryViewModelWrapper(baseUrl: AppConfig.apiBaseUrl, filterViewModel: filterViewModel)
        )
    }

    var body: some View {
        NavigationStack {
            EventFeedView(viewModel: viewModel, queryViewModel: queryViewModel)
                .navigationDestination(for: String.self) { eventId in
                    EventDetailView(
                        viewModel: EventDetailViewModelWrapper(eventId: eventId, baseUrl: AppConfig.apiBaseUrl),
                        baseUrl: AppConfig.apiBaseUrl
                    )
                }
        }
    }
}
