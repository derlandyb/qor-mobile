import Shared
import SwiftUI

@MainActor
final class EventFeedViewModelWrapper: ObservableObject {
    @Published private(set) var state: EventFeedUiState = EventFeedUiState(
        groupedEvents: [],
        isLoadingInitial: false,
        isLoadingMore: false,
        error: nil,
        endReached: false
    )

    private let iosViewModel: IosEventFeedViewModel
    private var watchHandle: Closeable?

    init(baseUrl: String) {
        iosViewModel = IosEventFeedViewModel(baseUrl: baseUrl)
        watchHandle = iosViewModel.watch { [weak self] newState in
            DispatchQueue.main.async {
                self?.state = newState
            }
        }
    }

    deinit {
        watchHandle?.close()
        iosViewModel.close()
    }

    func loadNextPage() {
        iosViewModel.loadNextPage()
    }

    func retry() {
        iosViewModel.retry()
    }
}
