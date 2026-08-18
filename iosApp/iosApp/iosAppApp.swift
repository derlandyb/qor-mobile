import SwiftUI

@main
struct iosAppApp: App {
    private let filterOwner = SharedFilterOwner(baseUrl: AppConfig.apiBaseUrl)

    @State private var mapPath = NavigationPath()

    var body: some Scene {
        WindowGroup {
            TabView {
                NavigationStack {
                    EventFeedView(
                        viewModel: EventFeedViewModelWrapper(baseUrl: AppConfig.apiBaseUrl),
                        queryViewModel: FeedQueryViewModelWrapper(
                            baseUrl: AppConfig.apiBaseUrl,
                            filterViewModel: filterOwner.filterViewModel
                        )
                    )
                    .navigationDestination(for: String.self) { eventId in
                        EventDetailView(
                            viewModel: EventDetailViewModelWrapper(eventId: eventId, baseUrl: AppConfig.apiBaseUrl),
                            baseUrl: AppConfig.apiBaseUrl
                        )
                    }
                }
                .tabItem { Label("Início", systemImage: "house") }

                NavigationStack(path: $mapPath) {
                    MapScreen(
                        viewModel: MapQueryViewModelWrapper(
                            baseUrl: AppConfig.apiBaseUrl,
                            filterViewModel: filterOwner.filterViewModel
                        ),
                        onOpenEventDetails: { eventId in mapPath.append(eventId) },
                        onClearFilters: { filterOwner.filterViewModel.clearAll() }
                    )
                    .navigationDestination(for: String.self) { eventId in
                        EventDetailView(
                            viewModel: EventDetailViewModelWrapper(eventId: eventId, baseUrl: AppConfig.apiBaseUrl),
                            baseUrl: AppConfig.apiBaseUrl
                        )
                    }
                }
                .tabItem { Label("Mapa", systemImage: "map") }
            }
        }
    }
}
