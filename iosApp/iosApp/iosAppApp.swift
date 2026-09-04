import SwiftUI
import shared

/// I1 — app entry point. Bootstraps the shared Koin DI graph (`di.doInitKoin()`, shared with
/// Android per `feat(mobile-shared)`) once at launch, then hosts a `NavigationStack` root.
/// Screens (I7-I14) push their own destinations onto this stack; there is no product content
/// here yet — that's out of scope for the I1-I6 foundation slice.
@main
struct IosAppApp: App {
    init() {
        KoinHelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            NavigationStack {
                Text("QOR")
            }
        }
    }
}
