import Shared
import SwiftUI

/// Owns the Map tab's `@StateObject` wrapper and its own `NavigationPath`, so pushing an event detail from
/// a marker tap doesn't invalidate `iosAppApp`'s scene body — which previously tore down and recreated both
/// `MapQueryViewModelWrapper` (cancelling the in-flight markers fetch/watch) and the unrelated Feed tab's
/// wrappers on every "Ver detalhes" tap, since `mapPath` lived on the App struct itself.
struct MapTabView: View {
    @StateObject private var viewModel: MapQueryViewModelWrapper
    @State private var path = NavigationPath()

    private let filterViewModel: FilterViewModel

    init(filterViewModel: FilterViewModel) {
        self.filterViewModel = filterViewModel
        _viewModel = StateObject(
            wrappedValue: MapQueryViewModelWrapper(baseUrl: AppConfig.apiBaseUrl, filterViewModel: filterViewModel)
        )
    }

    var body: some View {
        NavigationStack(path: $path) {
            MapScreen(
                viewModel: viewModel,
                onOpenEventDetails: { eventId in path.append(eventId) },
                onClearFilters: { filterViewModel.clearAll() }
            )
            .navigationDestination(for: String.self) { eventId in
                EventDetailView(
                    viewModel: EventDetailViewModelWrapper(eventId: eventId, baseUrl: AppConfig.apiBaseUrl),
                    baseUrl: AppConfig.apiBaseUrl
                )
            }
        }
    }
}
