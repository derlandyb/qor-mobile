import SwiftUI
import shared

/// I12 — placeholder genre catalog (DISC-15), mirrors Android's own `ExploreScreen.PlaceholderGenres`
/// and `GenreTagColors`' known-genre set: `Event.genre` is a raw API string with no genre-lookup
/// endpoint yet (ARCHITECTURE §14's DB-backed lookup table isn't exposed to clients).
private let placeholderGenres = ["Rock", "Samba", "Sertanejo", "Eletrônico", "Reggae"]

/// A12/I12 parity — UI state for [ExploreView], structurally identical to Android's own
/// `HomeFeedUiState` (reused there by `ExploreViewModel` rather than duplicated): the event-list
/// surface with filters, loading/empty/error states, and pagination-in-flight/refresh-in-flight
/// flags on the content case.
enum ExploreUiState: Equatable {
    case loading
    case content(events: [Event], isLoadingMore: Bool = false, isRefreshing: Bool = false)
    case empty
    case error
}

/// I12 — Explore tab (DISC-14–DISC-18): the same public event list as [HomeFeedView], with
/// city/genre filters on top, as its own BottomNav destination. Reuses [HomeFeedView]'s
/// `HomeFeedEventsGateway`/`HomeFeedPollingGateway` seam (their production implementations each
/// resolve a *fresh* `PollingCoordinator` — a Koin `factory`, not `single`, per AD-021/STATE.md —
/// so this never fights `HomeFeedViewModel` over shared poll state) rather than a second,
/// independent Kotlin-side bridge: consolidating onto one `StateFlow`-to-Swift mechanism instead
/// of two, and giving this view model the same fake-injectable seam every other I7-I14 screen
/// has (its earlier direct `IosDependencies.shared` construction had no test seam at all, which
/// let every `ExploreViewModel()` in a test process start a real, unbounded background network
/// poll loop hitting the real API host — harmless alone, but destabilizing once the full suite
/// runs together).
@MainActor
final class ExploreViewModel: ObservableObject {
    @Published private(set) var uiState: ExploreUiState = .loading
    @Published private(set) var selectedCity: City?
    @Published private(set) var selectedGenre: String?

    private let eventsGateway: HomeFeedEventsGateway
    private let pollingGateway: HomeFeedPollingGateway

    private var nextCursor: String?
    private var loadTask: Task<Void, Never>?

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
        loadInitial()
    }

    deinit {
        pollingGateway.stop()
        loadTask?.cancel()
    }

    /// DISC-14 — toggles `city`: selecting the already-active city clears it (back to unfiltered).
    func onCitySelected(_ city: City) {
        selectedCity = (selectedCity == city) ? nil : city
        applyFilters()
    }

    /// DISC-15 — toggles `genre`: selecting the already-active genre clears it (back to unfiltered).
    func onGenreSelected(_ genre: String) {
        selectedGenre = (selectedGenre == genre) ? nil : genre
        applyFilters()
    }

    /// DISC-17 — clears both filters, returning to the default unfiltered soonest-first list.
    func onClearFilters() {
        selectedCity = nil
        selectedGenre = nil
        applyFilters()
    }

    /// DISC-18 pagination — call when the list scrolls near its end. No-op with no next page or a
    /// fetch already in flight.
    func onLoadMore() {
        guard case .content(let events, let isLoadingMore, let isRefreshing) = uiState,
              !isLoadingMore, let cursor = nextCursor else { return }

        uiState = .content(events: events, isLoadingMore: true, isRefreshing: isRefreshing)
        Task { @MainActor in
            do {
                let page = try await eventsGateway.loadUpcoming(
                    city: selectedCity, genre: selectedGenre, cursor: cursor
                )
                nextCursor = page.nextCursor
                if case .content(let current, _, let refreshing) = uiState {
                    uiState = .content(events: current + page.events, isLoadingMore: false, isRefreshing: refreshing)
                }
            } catch {
                // Network-boundary catch-all, mirrors Android: pagination fails silently (spinner
                // just stops) rather than surfacing a dedicated error state.
                if case .content(let current, _, let refreshing) = uiState {
                    uiState = .content(events: current, isLoadingMore: false, isRefreshing: refreshing)
                }
            }
        }
    }

    /// DISC-04's manual pull-to-refresh trigger — an immediate out-of-band fetch via
    /// [HomeFeedPollingGateway.refreshNow].
    func onRefresh() {
        if case .content(let events, let isLoadingMore, _) = uiState {
            uiState = .content(events: events, isLoadingMore: isLoadingMore, isRefreshing: true)
        }
        Task { @MainActor in
            do {
                try await pollingGateway.refreshNow()
            } catch {
                if case .content(let events, let isLoadingMore, _) = uiState {
                    uiState = .content(events: events, isLoadingMore: isLoadingMore, isRefreshing: false)
                } else {
                    uiState = .error
                }
            }
        }
    }

    private func applyFilters() {
        nextCursor = nil
        loadInitial()
        pollingGateway.start(city: selectedCity, genre: selectedGenre)
    }

    private func loadInitial() {
        loadTask?.cancel()
        uiState = .loading
        loadTask = Task { @MainActor in
            do {
                let page = try await eventsGateway.loadUpcoming(city: selectedCity, genre: selectedGenre, cursor: nil)
                guard !Task.isCancelled else { return }
                applyPage(page)
            } catch {
                guard !Task.isCancelled else { return }
                uiState = .error
            }
        }
    }

    private func applyPage(_ page: EventPage) {
        nextCursor = page.nextCursor
        uiState = page.events.isEmpty ? .empty : .content(events: page.events)
    }
}

/// I12 — Explore tab view (DISC-14–DISC-18), Stitch screen `642def01ae144e1f8a1896018febf379`.
struct ExploreView: View {
    let onEventClick: (Event) -> Void
    var onMapClick: (Event) -> Void = { _ in }

    @StateObject private var viewModel: ExploreViewModel

    init(
        onEventClick: @escaping (Event) -> Void,
        onMapClick: @escaping (Event) -> Void = { _ in },
        viewModel: ExploreViewModel? = nil
    ) {
        self.onEventClick = onEventClick
        self.onMapClick = onMapClick
        _viewModel = StateObject(wrappedValue: viewModel ?? ExploreViewModel())
    }

    private var hasActiveFilters: Bool { viewModel.selectedCity != nil || viewModel.selectedGenre != nil }

    var body: some View {
        VStack(spacing: 0) {
            CityFilterBar(selected: viewModel.selectedCity, onSelect: viewModel.onCitySelected)
            GenreFilterRow(selected: viewModel.selectedGenre, onSelect: viewModel.onGenreSelected)

            switch viewModel.uiState {
            case .loading:
                loadingIndicator

            case .empty:
                exploreEmptyState

            case .error:
                errorState

            case .content(let events, let isLoadingMore, let isRefreshing):
                contentList(events: events, isLoadingMore: isLoadingMore, isRefreshing: isRefreshing)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityIdentifier("explore_screen")
    }

    @ViewBuilder
    private func contentList(events: [Event], isLoadingMore: Bool, isRefreshing: Bool) -> some View {
        ScrollView {
            LazyVStack(spacing: QorSpace.space4) {
                ForEach(events, id: \.id) { event in
                    EventCard(
                        event: event,
                        onClick: { onEventClick(event) },
                        onMapClick: { onMapClick(event) }
                    )
                    .onAppear {
                        // DISC-18 — mirrors Android's last-3-items pagination threshold.
                        if let index = events.firstIndex(where: { $0.id == event.id }), index >= events.count - 3 {
                            viewModel.onLoadMore()
                        }
                    }
                }

                if isLoadingMore {
                    ProgressView().tint(QorColor.accentPink)
                }
            }
            .padding(QorSpace.space4)
        }
        .refreshable { viewModel.onRefresh() }
    }

    private var loadingIndicator: some View {
        VStack {
            Spacer()
            ProgressView().tint(QorColor.accentPink)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var exploreEmptyState: some View {
        VStack(spacing: QorSpace.space3) {
            Spacer()
            EmptyState(
                message: hasActiveFilters
                    ? String(localized: "explore_empty_state_no_matches")
                    : String(localized: "empty_state_no_events")
            )
            if hasActiveFilters {
                SecondaryButton(
                    text: String(localized: "explore_cta_limpar_filtros"), onClick: viewModel.onClearFilters
                )
                .padding(.horizontal, QorSpace.space6)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var errorState: some View {
        VStack(spacing: QorSpace.space3) {
            Spacer()
            Text(String(localized: "explore_error_message"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)
                .multilineTextAlignment(.center)
            PrimaryButton(text: String(localized: "explore_cta_tentar_novamente"), onClick: viewModel.onRefresh)
                .padding(.horizontal, QorSpace.space6)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(QorSpace.space6)
    }
}

/// DISC-15 — a horizontally-scrolling, single-select row of [GenreTag] chips over [placeholderGenres].
private struct GenreFilterRow: View {
    let selected: String?
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: QorSpace.space2) {
                ForEach(placeholderGenres, id: \.self) { genre in
                    let isActive = genre == selected
                    GenreTag(genre: genre)
                        .overlay(
                            RoundedRectangle(cornerRadius: QorRadius.radiusSm)
                                .stroke(
                                    isActive ? QorColor.accentPink : .clear,
                                    lineWidth: CGFloat(QualORockThemeTokens.shared.BorderWidthHairlineDp)
                                )
                        )
                        .onTapGesture { onSelect(genre) }
                        .accessibilityAddTraits(isActive ? [.isSelected] : [])
                }
            }
            .padding(.horizontal, QorSpace.space3)
            .padding(.vertical, QorSpace.space2)
        }
    }
}
