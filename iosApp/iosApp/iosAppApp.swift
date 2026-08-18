import SwiftUI

@main
struct iosAppApp: App {
    private let filterOwner = SharedFilterOwner(baseUrl: AppConfig.apiBaseUrl)

    var body: some Scene {
        WindowGroup {
            TabView {
                FeedTabView(filterViewModel: filterOwner.filterViewModel)
                    .tabItem { Label("Início", systemImage: "house") }

                MapTabView(filterViewModel: filterOwner.filterViewModel)
                    .tabItem { Label("Mapa", systemImage: "map") }
            }
        }
    }
}
