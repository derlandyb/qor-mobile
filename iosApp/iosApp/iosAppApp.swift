import SwiftUI

/// Bare SwiftUI entry point — enough for the KMP `shared` framework to link and for
/// `xcodebuild -scheme iosApp build` to succeed. Product screens are built in the iOS UI
/// tasks (I1-I14), out of scope for the Shared module foundation.
@main
struct iosAppApp: App {
    var body: some Scene {
        WindowGroup {
            Text("QOR")
        }
    }
}
