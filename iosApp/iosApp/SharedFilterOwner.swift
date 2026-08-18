import Shared
import SwiftUI

/// Holds the single `FilterViewModel` instance shared by the Feed and Map tabs (see `TabView` in
/// `iosAppApp.swift`), so active filters survive switching tabs (MAP-003 AC2). Not itself observable —
/// `FeedQueryViewModelWrapper`/`MapQueryViewModelWrapper` each watch the shared `filterViewModel` and
/// publish their own `@Published` state.
@MainActor
final class SharedFilterOwner: ObservableObject {
    private let iosSharedFilterViewModel: IosSharedFilterViewModel
    var filterViewModel: FilterViewModel { iosSharedFilterViewModel.filterViewModel }

    init(baseUrl: String) {
        iosSharedFilterViewModel = IosSharedFilterViewModel(baseUrl: baseUrl)
    }

    deinit {
        iosSharedFilterViewModel.close()
    }
}
