package di

import domain.enum.City
import domain.event.EventPage
import domain.event.PollingCoordinator
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * I11/I12 — Swift-friendly, closure-based bridge over [PollingCoordinator.events] (a raw
 * `StateFlow`) that also owns starting/stopping the poll loop itself. This project has no SKIE/
 * KMP-NativeCoroutines dependency wired in — reading the generated `shared.h` confirms Kotlin's
 * default Objective-C export only gives `Flow` a completion-handler
 * `collect(collector:completionHandler:)`, which never returns for an infinite flow and has no
 * idiomatic Swift `async`/`await`/`AsyncSequence` shape. Rather than hand-rolling a
 * `FlowCollector` conformance in every Swift screen that needs to observe a poll loop, this
 * collects [PollingCoordinator.events] once, in Kotlin, and republishes each value to a plain
 * Swift closure via [watch].
 *
 * [PollingCoordinator.start] also needs a Kotlin `CoroutineScope`, which has no idiomatic Swift
 * construction either — [start] below keeps that entirely on the Kotlin side, passing this
 * watcher's own [scope], so no iOS screen ever has to construct or hold one.
 *
 * [watch] delivers the flow's *current* value synchronously on subscription (a `StateFlow`
 * replay), then every subsequent emission — callers that need to ignore that first, possibly
 * stale replay (mirroring Android `HomeFeedViewModel`'s `events.drop(1)` — see its KDoc for why)
 * should skip their own first callback invocation themselves.
 *
 * One instance per screen (Home, Explore) — each wraps its own [PollingCoordinator] instance
 * (a Koin `factory`, per AD-021/STATE.md) so Home and Explore never fight over shared poll state.
 */
class PollingEventsWatcher(private val coordinator: PollingCoordinator) {
    private val scope = MainScope()
    private var job: Job? = null

    fun watch(onEach: (EventPage) -> Unit) {
        job?.cancel()
        job = scope.launch {
            coordinator.events.collect { page -> onEach(page) }
        }
    }

    /** Starts/restarts [coordinator]'s poll loop against the given filters, using this watcher's own scope. */
    fun start(city: City? = null, genre: String? = null) {
        coordinator.start(scope, city, genre)
    }

    /** Immediate out-of-band fetch (pull-to-refresh) — suspend, bridges to Swift as a completion-handler call. */
    suspend fun refreshNow() {
        coordinator.refreshNow()
    }

    /** Stops observing and stops [coordinator]'s poll loop; safe to call more than once. */
    fun stop() {
        job?.cancel()
        job = null
        coordinator.stop()
    }

    /** Fully tears down this watcher's own scope — call once, when the owning screen is done. */
    fun close() {
        stop()
        scope.cancel()
    }
}
