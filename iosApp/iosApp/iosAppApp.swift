import SwiftUI
import shared

/// I1 — app entry point. Bootstraps the shared Koin DI graph (`di.doInitKoin()`, shared with
/// Android per `feat(mobile-shared)`) once at launch, then hosts [AppNavigation] (I14) as the
/// root — the full MVP Core route table.
@main
struct IosAppApp: App {
    init() {
        #if DEBUG
        let isDebugBuild = true
        #else
        let isDebugBuild = false
        #endif
        KoinHelperKt.doInitKoin(isDebug: isDebugBuild)
    }

    var body: some Scene {
        WindowGroup {
            AppNavigation()
        }
    }
}
