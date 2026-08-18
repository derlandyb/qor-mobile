import SwiftUI

@main
struct iosAppApp: App {
    var body: some Scene {
        WindowGroup {
            NavigationStack {
                EventFeedView(
                    viewModel: EventFeedViewModelWrapper(baseUrl: AppConfig.apiBaseUrl),
                    queryViewModel: FeedQueryViewModelWrapper(baseUrl: AppConfig.apiBaseUrl)
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
}
