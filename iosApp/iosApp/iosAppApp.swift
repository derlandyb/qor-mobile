import SwiftUI

@main
struct iosAppApp: App {
    var body: some Scene {
        WindowGroup {
            EventFeedView(
                viewModel: EventFeedViewModelWrapper(baseUrl: AppConfig.apiBaseUrl)
            )
        }
    }
}
