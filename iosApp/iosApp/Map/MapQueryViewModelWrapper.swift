import Shared
import SwiftUI

@MainActor
final class MapQueryViewModelWrapper: ObservableObject {
    @Published private(set) var markersState: MapMarkersUiState = MapMarkersUiStateLoading.shared

    private let iosViewModel: IosMapQueryViewModel
    private var watchHandle: Closeable?

    init(baseUrl: String, filterViewModel: FilterViewModel) {
        iosViewModel = IosMapQueryViewModel(baseUrl: baseUrl, filterViewModel: filterViewModel)
        watchHandle = iosViewModel.watchMarkers { [weak self] newState in
            DispatchQueue.main.async { self?.markersState = newState }
        }
    }

    deinit {
        watchHandle?.close()
        iosViewModel.close()
    }

    func retry() {
        iosViewModel.retry()
    }
}
