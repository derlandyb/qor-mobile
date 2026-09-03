import XCTest

/// Trivial placeholder so `xcodebuild test -scheme iosApp` has something to run (S2). Real
/// SwiftUI unit tests land with the iOS UI tasks (I1-I14), out of scope for the Shared module
/// foundation.
final class iosAppTests: XCTestCase {
    func testPlaceholderAlwaysPasses() {
        XCTAssertTrue(true)
    }
}
