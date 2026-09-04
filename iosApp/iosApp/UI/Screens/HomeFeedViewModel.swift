import Foundation
import shared

/// UI state for `HomeFeedView` (I11, DISC-01–DISC-06). Mirrors Android's own
/// `HomeFeedUiState` sealed class field-for-field.
enum HomeFeedUiState {
    case loading

    /// `isLoadingMore` drives the trailing pagination spinner; `isRefreshing` drives the
    /// pull-to-refresh indicator. Both default `false` so the common case (a fresh page) stays a
    /// minimal constructor call, same as Android's `Content(events)` default-arg shorthand.
    case content(events: [Event], isLoadingMore: Bool = false, isRefreshing: Bool = false)

    case empty

    /// `EventRepository`/`ListUpcomingEvents` carry no server-message contract to show verbatim,
    /// just one generic pt-BR failure case — `HomeFeedView` resolves the copy from
    /// `home_feed_error_message`, keeping this view model UI-resource-agnostic.
    case error
}

/// Abstracts the page-1/pagination fetch away from the concrete (`objc_subclassing_restricted`,
/// so un-fakeable directly) `SharedListUpcomingEvents` class, so tests can substitute a fake
/// without touching Kotlin/Native interop at all.
protocol HomeFeedEventsGateway {
    func loadUpcoming(city: City?, genre: String?, cursor: String?) async throws -> EventPage
}

/// Same reasoning as `HomeFeedEventsGateway`, for `PollingCoordinator` (also
/// `objc_subclassing_restricted`).
protocol HomeFeedPollingGateway: AnyObject {
    func start(city: City?, genre: String?)
    func stop()
    func refreshNow() async throws
    /// The coordinator's current page, read synchronously — mirrors Android's
    /// `pollingCoordinator.events.value`.
    func currentPage() -> EventPage
    /// Registers the one live callback for every later page (mirrors Android's
    /// `pollingCoordinator.events.drop(1).collect { ... }`) — never replays the coordinator's
    /// stale/default value at registration time.
    func observeUpdates(_ onUpdate: @escaping (EventPage) -> Void)
}

enum HomeFeedGatewayError: Error {
    case missingResult
}

/// Production `HomeFeedEventsGateway` wrapping `IosDependencies.shared.listUpcomingEvents()`.
/// Bridges its completion-handler-based `execute` (no SKIE in this project — see
/// `Support/KmpCoroutineBridging.swift`) into `async`/`await`.
final class SharedHomeFeedEventsGateway: HomeFeedEventsGateway {
    private let listUpcomingEvents = IosDependencies.shared.listUpcomingEvents()

    func loadUpcoming(city: City?, genre: String?, cursor: String?) async throws -> EventPage {
        try await withCheckedThrowingContinuation { continuation in
            listUpcomingEvents.execute(city: city, genre: genre, cursor: cursor) { page, error in
                if let page {
                    continuation.resume(returning: page)
                } else {
                    continuation.resume(throwing: error ?? HomeFeedGatewayError.missingResult)
                }
            }
        }
    }
}

/// Production `HomeFeedPollingGateway`. **Obtains its own fresh `PollingCoordinator` instance**
/// via `IosDependencies.shared.pollingCoordinator()` — per AD-021 (STATE.md), that accessor is
/// backed by a Koin `factory`, not `single`, exactly so Home and Explore (which can both be alive
/// at once) never share/fight over one poll loop's state. Never cache/share this gateway (or a
/// `PollingCoordinator`) across screens.
final class SharedHomeFeedPollingGateway: HomeFeedPollingGateway {
    private let coordinator = IosDependencies.shared.pollingCoordinator()
    private let scope = KmpCoroutineScope()

    func start(city: City?, genre: String?) {
        coordinator.start(scope: scope, city: city, genre: genre)
    }

    func stop() {
        coordinator.stop()
    }

    func refreshNow() async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            coordinator.refreshNow { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume()
                }
            }
        }
    }

    func currentPage() -> EventPage {
        (coordinator.events.value as? EventPage) ?? EventPage(events: [], nextCursor: nil)
    }

    func observeUpdates(_ onUpdate: @escaping (EventPage) -> Void) {
        let collector = ClosureFlowCollector<EventPage>(dropFirst: 1, onEmit: onUpdate)
        coordinator.events.collect(collector: collector) { _ in }
    }
}

/// I11 — public event feed (DISC-01–DISC-06), no login required. The initial load and
/// `onLoadMore` pagination call `HomeFeedEventsGateway` directly (both need per-call error
/// visibility a plain enum state can show); `HomeFeedPollingGateway` drives the shared 30s
/// live-refresh loop (`QorConfig.EventListPollIntervalSeconds`) and backs `onRefresh`'s manual
/// pull-to-refresh trigger — same split as Android's `HomeFeedViewModel.kt`, whose KDoc documents
/// the reasoning in full (dropping the coordinator's first/stale replay, resetting to page 1 on
/// every tick, isolating the poll loop's uncaught internal failures, etc.) and applies unchanged
/// here.
///
/// Navigation is explicitly **not** this view model's job (I14's, later) — it exposes no
/// navigation callbacks of its own; `HomeFeedView` owns the `onEventClick`/`onMapClick` closures
/// instead, same navigation-agnostic split as Android's `HomeFeedScreen`.
@MainActor
final class HomeFeedViewModel: ObservableObject {
    @Published private(set) var uiState: HomeFeedUiState = .loading

    private let eventsGateway: HomeFeedEventsGateway
    private let pollingGateway: HomeFeedPollingGateway
    private var nextCursor: String?

    private var initialLoadTask: Task<Void, Never>?
    private var loadMoreTask: Task<Void, Never>?
    private var refreshTask: Task<Void, Never>?

    init(
        eventsGateway: HomeFeedEventsGateway = SharedHomeFeedEventsGateway(),
        pollingGateway: HomeFeedPollingGateway = SharedHomeFeedPollingGateway()
    ) {
        self.eventsGateway = eventsGateway
        self.pollingGateway = pollingGateway

        pollingGateway.observeUpdates { [weak self] page in
            self?.applyPage(page)
        }
        pollingGateway.start(city: nil, genre: nil)

        initialLoadTask = Task { [weak self] in
            await self?.loadInitial()
        }
    }

    deinit {
        pollingGateway.stop()
        initialLoadTask?.cancel()
        loadMoreTask?.cancel()
        refreshTask?.cancel()
    }

    /// DISC-02 pagination — call when the list scrolls near its end. No-op with no next page or a
    /// fetch already in flight.
    func onLoadMore() {
        guard case .content(let events, let isLoadingMore, let isRefreshing) = uiState,
              !isLoadingMore,
              let cursor = nextCursor
        else { return }

        uiState = .content(events: events, isLoadingMore: true, isRefreshing: isRefreshing)

        loadMoreTask = Task { [weak self] in
            guard let self else { return }
            do {
                let page = try await self.eventsGateway.loadUpcoming(city: nil, genre: nil, cursor: cursor)
                self.nextCursor = page.nextCursor
                let merged = events + page.events
                self.uiState = .content(events: merged, isLoadingMore: false, isRefreshing: isRefreshing)
            } catch is CancellationError {
                // Screen went away mid-fetch — nothing left to update.
            } catch {
                // Network-boundary catch-all, same as Android: no typed failure contract to
                // surface, and pagination fails silently (spinner just stops) rather than
                // flipping the whole screen to an error state.
                self.uiState = .content(events: events, isLoadingMore: false, isRefreshing: isRefreshing)
            }
        }
    }

    /// DISC-04's manual pull-to-refresh trigger — an immediate out-of-band fetch via
    /// `HomeFeedPollingGateway.refreshNow()`. Fire-and-forget entry point for non-async call
    /// sites (e.g. the error state's "Tentar novamente" button).
    func onRefresh() {
        refreshTask = Task { [weak self] in
            await self?.refresh()
        }
    }

    /// Same as `onRefresh()`, but awaitable — what `HomeFeedView`'s `.refreshable { }` calls, so
    /// SwiftUI's pull-to-refresh spinner stays visible for the fetch's real duration instead of
    /// dismissing immediately.
    func refresh() async {
        if case .content(let events, let isLoadingMore, _) = uiState {
            uiState = .content(events: events, isLoadingMore: isLoadingMore, isRefreshing: true)
        }

        do {
            try await pollingGateway.refreshNow()
            applyPage(pollingGateway.currentPage())
        } catch is CancellationError {
            // Screen went away mid-refresh — nothing left to update.
        } catch {
            if case .content(let events, let isLoadingMore, _) = uiState {
                uiState = .content(events: events, isLoadingMore: isLoadingMore, isRefreshing: false)
            } else {
                uiState = .error
            }
        }
    }

    private func loadInitial() async {
        uiState = .loading
        do {
            let page = try await eventsGateway.loadUpcoming(city: nil, genre: nil, cursor: nil)
            applyPage(page)
        } catch is CancellationError {
            // Screen went away mid-load — nothing left to update.
        } catch {
            uiState = .error
        }
    }

    private func applyPage(_ page: EventPage) {
        nextCursor = page.nextCursor
        uiState = page.events.isEmpty
            ? .empty
            : .content(events: page.events, isLoadingMore: false, isRefreshing: false)
    }
}
