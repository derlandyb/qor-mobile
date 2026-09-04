import SwiftUI
import shared

/// How many items from the end of the list trigger `HomeFeedViewModel.onLoadMore` (DISC-02) —
/// mirrors Android's `LoadMoreThresholdItems`.
private let loadMoreThresholdItems = 3

/// I11 — the public event feed (DISC-01–DISC-06), no login required. Owns only rendering +
/// scroll pagination + pull-to-refresh wiring; navigation on card tap is I14's job, not this
/// screen's — `onEventClick`/`onMapClick` stay plain callbacks, same navigation-agnostic design
/// as Android's `HomeFeedScreen`.
///
/// `onMapClick` defaults to a no-op: `EventCard` requires a map-CTA callback (design-system.md
/// §4.1's "Ver no Mapa"), but building the actual maps deep link from `Event.address` is left to
/// whoever wires this screen into the nav flow (I14), not this screen's own concern.
struct HomeFeedView: View {
    let onEventClick: (String) -> Void
    var onMapClick: (Event) -> Void = { _ in }

    @StateObject private var viewModel: HomeFeedViewModel

    init(
        onEventClick: @escaping (String) -> Void,
        onMapClick: @escaping (Event) -> Void = { _ in },
        viewModel: HomeFeedViewModel? = nil
    ) {
        self.onEventClick = onEventClick
        self.onMapClick = onMapClick
        _viewModel = StateObject(wrappedValue: viewModel ?? HomeFeedViewModel())
    }

    var body: some View {
        Group {
            switch viewModel.uiState {
            case .loading:
                loadingIndicator
            case .empty:
                emptyState
            case .error:
                errorState
            case .content(let events, let isLoadingMore, _):
                eventList(events: events, isLoadingMore: isLoadingMore)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(QorColor.bgDeep)
    }

    private var loadingIndicator: some View {
        ProgressView()
            .tint(QorColor.accentPink)
            .accessibilityIdentifier("home_feed_loading")
    }

    private var emptyState: some View {
        EmptyState()
            .accessibilityIdentifier("home_feed_empty")
    }

    private var errorState: some View {
        VStack(spacing: QorSpace.space3) {
            Text(String(localized: "home_feed_error_message"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)

            PrimaryButton(
                text: String(localized: "home_feed_cta_tentar_novamente"),
                onClick: { viewModel.onRefresh() }
            )
            .accessibilityIdentifier("home_feed_retry")
        }
        .padding(QorSpace.space6)
        .accessibilityIdentifier("home_feed_error")
    }

    private func eventList(events: [Event], isLoadingMore: Bool) -> some View {
        ScrollView {
            LazyVStack(spacing: QorSpace.space4) {
                ForEach(Array(events.enumerated()), id: \.element.id) { index, event in
                    EventCard(
                        event: event,
                        onClick: { onEventClick(event.id) },
                        onMapClick: { onMapClick(event) }
                    )
                    .entranceStagger(index: index)
                    .accessibilityIdentifier("home_feed_event_\(event.id)")
                    .onAppear {
                        if index >= events.count - loadMoreThresholdItems {
                            viewModel.onLoadMore()
                        }
                    }
                }

                if isLoadingMore {
                    ProgressView()
                        .tint(QorColor.accentPink)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, QorSpace.space3)
                        .accessibilityIdentifier("home_feed_loading_more")
                }
            }
            .padding(QorSpace.space4)
        }
        .accessibilityIdentifier("home_feed_list")
        .refreshable {
            await viewModel.refresh()
        }
    }
}
