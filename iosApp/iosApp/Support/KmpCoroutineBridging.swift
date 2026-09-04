import Foundation
import shared

/// I11 — this project has no SKIE (`shared/build.gradle.kts` doesn't apply it), so Kotlin/Native's
/// plain Objective-C export is what iOS screens bridge against: `suspend fun`s become
/// completion-handler methods (wrapped below into `async`/`await` per call site, e.g.
/// `SharedHomeFeedEventsGateway.loadUpcoming`), and `Flow<T>`/`StateFlow<T>` become a `collect
/// (collector:completionHandler:)` method expecting a `Kotlinx_coroutines_coreFlowCollector`
/// conformance — there is no native `AsyncSequence` bridge. The two small conformances below
/// are the reusable piece of that bridge every screen that touches `PollingCoordinator` (or any
/// other API expecting a Kotlin `CoroutineScope`/`Flow`) needs; this is the first screen to need
/// it, so it lives here rather than duplicated per-screen.

/// A `CoroutineContext` with no elements — mirrors `kotlin.coroutines.EmptyCoroutineContext`'s own
/// behavior (`fold` returns `initial` untouched, `get` always misses, `minusKey` is a no-op,
/// `plus` yields entirely to the other side). Kotlin's `CoroutineScope.launch` builder still works
/// against it: with no `ContinuationInterceptor` element present it falls back to its own default
/// dispatcher, exactly as if this were the real `EmptyCoroutineContext`.
final class EmptyKotlinCoroutineContext: NSObject, KotlinCoroutineContext {
    func fold(initial: Any?, operation: (Any?, KotlinCoroutineContextElement) -> Any?) -> Any? {
        initial
    }

    func get(key: KotlinCoroutineContextKey) -> KotlinCoroutineContextElement? {
        nil
    }

    func minusKey(key: KotlinCoroutineContextKey) -> KotlinCoroutineContext {
        self
    }

    func plus(context: KotlinCoroutineContext) -> KotlinCoroutineContext {
        context
    }
}

/// A bare `CoroutineScope` Swift code can hand to a Kotlin API that expects one (e.g.
/// `PollingCoordinator.start(scope:city:genre:)`) without depending on SKIE. Deliberately carries
/// no `Job` element: cancelling whatever was `launch`-ed against it is the *Kotlin* callee's own
/// responsibility (`PollingCoordinator.stop()` cancels its own stored `Job` reference internally),
/// not something this scope tracks.
final class KmpCoroutineScope: NSObject, Kotlinx_coroutines_coreCoroutineScope {
    let coroutineContext: KotlinCoroutineContext = EmptyKotlinCoroutineContext()
}

/// Bridges a Kotlin `Flow<T>`'s completion-handler-based `collect` into a plain Swift closure.
/// `dropFirst` mirrors Kotlin's `Flow.drop(n)` — a `StateFlow` replays its *current* value
/// synchronously to any new collector before any real update arrives, so a screen collecting
/// `PollingCoordinator.events` skips that stale/default replay the same way
/// `HomeFeedViewModel.kt`'s Android counterpart does with `.drop(1)`. Always hops to the main
/// queue before calling `onEmit`, since Kotlin's own collection runs off-main.
final class ClosureFlowCollector<T: AnyObject>: NSObject, Kotlinx_coroutines_coreFlowCollector {
    private let onEmit: (T) -> Void
    private var remainingDrops: Int

    init(dropFirst: Int = 0, onEmit: @escaping (T) -> Void) {
        self.remainingDrops = dropFirst
        self.onEmit = onEmit
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        defer { completionHandler(nil) }

        guard remainingDrops <= 0 else {
            remainingDrops -= 1
            return
        }
        guard let typed = value as? T else { return }

        let onEmit = self.onEmit
        DispatchQueue.main.async {
            onEmit(typed)
        }
    }
}
