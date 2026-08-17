import Shared
import SwiftUI

@MainActor
final class EventDetailViewModelWrapper: ObservableObject {
    @Published private(set) var state: EventDetailUiState = EventDetailUiStateLoading.shared

    private let iosViewModel: IosEventDetailViewModel
    private var watchHandle: Closeable?

    init(eventId: String, baseUrl: String) {
        iosViewModel = IosEventDetailViewModel(eventId: eventId, baseUrl: baseUrl)
        watchHandle = iosViewModel.watch { [weak self] newState in
            DispatchQueue.main.async {
                self?.state = newState
            }
        }
        iosViewModel.load()
    }

    deinit {
        watchHandle?.close()
        iosViewModel.close()
    }

    func retry() {
        iosViewModel.retry()
    }
}
