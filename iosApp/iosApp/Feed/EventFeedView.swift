import Shared
import SwiftUI

struct EventFeedView: View {
    @ObservedObject var viewModel: EventFeedViewModelWrapper
    @ObservedObject var queryViewModel: FeedQueryViewModelWrapper
    @State private var isPanelOpen = false

    var body: some View {
        VStack(spacing: 0) {
            SearchBarView(
                query: queryViewModel.query,
                onQueryChange: { queryViewModel.setQuery($0) },
                onClear: { queryViewModel.clearQuery() }
            )
            FilterBarView(
                state: queryViewModel.filterState,
                onDateSelect: { queryViewModel.selectDateBucket($0) },
                onCitySelect: { queryViewModel.selectCity(city: $0) },
                onOpenPanel: { isPanelOpen.toggle() }
            )
            if isPanelOpen {
                GenreArtistPanelView(
                    genreOptions: queryViewModel.genreOptions,
                    artistOptions: queryViewModel.artistOptions,
                    selectedGenres: queryViewModel.filterState.genres,
                    selectedArtist: queryViewModel.filterState.artist,
                    onToggleGenre: { queryViewModel.toggleGenre(genre: $0) },
                    onSelectArtist: { queryViewModel.selectArtist($0) }
                )
            }
            ActiveFilterChipsView(
                chips: queryViewModel.filterState.asChips(),
                onRemove: { queryViewModel.removeChip($0) },
                onClearAll: {
                    queryViewModel.clearAll()
                    queryViewModel.clearQuery()
                }
            )

            resultsRegion
        }
    }

    @ViewBuilder
    private var resultsRegion: some View {
        switch queryViewModel.resultsState {
        case is FeedResultsUiStateInactive:
            unfilteredFeed
        case is FeedResultsUiStateLoading:
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        case let results as FeedResultsUiStateResults:
            resultsList(events: results.events)
        case let noResults as FeedResultsUiStateNoResults:
            NoResultsStateView(filters: noResults.activeFilters, query: noResults.q) {
                queryViewModel.clearAll()
                queryViewModel.clearQuery()
            }
        case is FeedResultsUiStateError:
            ResultsErrorStateView { queryViewModel.retryResults() }
        default:
            EmptyView()
        }
    }

    @ViewBuilder
    private var unfilteredFeed: some View {
        if viewModel.state.isLoadingInitial {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if viewModel.state.error == FeedError.initialLoad {
            InitialErrorStateView { viewModel.retry() }
        } else if viewModel.state.isEmpty {
            EmptyStateView()
        } else {
            feedList
        }
    }

    private var feedList: some View {
        List {
            ForEach(viewModel.state.groupedEvents, id: \.label) { group in
                Section(header: Text(group.label)) {
                    ForEach(group.events, id: \.id) { event in
                        NavigationLink(value: event.id) {
                            EventCardView(event: event)
                        }
                        .onAppear {
                            if event.id == group.events.last?.id,
                               group.label == viewModel.state.groupedEvents.last?.label {
                                viewModel.loadNextPage()
                            }
                        }
                    }
                }
            }

            if viewModel.state.isLoadingMore {
                ProgressView()
                    .frame(maxWidth: .infinity)
            }

            if viewModel.state.error == FeedError.loadMore {
                Button("Tentar novamente") { viewModel.retry() }
                    .frame(maxWidth: .infinity)
            }

            if viewModel.state.endReached && viewModel.state.error != FeedError.loadMore {
                Text("Você já viu todos os shows por aqui")
                    .font(.footnote)
                    .frame(maxWidth: .infinity)
            }
        }
        .listStyle(.plain)
    }

    private func resultsList(events: [Event]) -> some View {
        List(events, id: \.id) { event in
            NavigationLink(value: event.id) {
                EventCardView(event: event)
            }
        }
        .listStyle(.plain)
    }
}

private struct InitialErrorStateView: View {
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Não foi possível carregar os shows")
                .font(.headline)
            Button("Tentar novamente", action: onRetry)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct EmptyStateView: View {
    var body: some View {
        Text("Nenhum show por aqui ainda")
            .font(.headline)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// FILTER-006 AC2: the zero-result empty state must name the applied filters/query.
func describeActiveQuery(filters: FilterState, query: String) -> String? {
    var parts: [String] = []
    if !query.isEmpty { parts.append("\"\(query)\"") }
    if let bucket = filters.dateBucket { parts.append(bucket.label) }
    if let city = filters.city { parts.append(city) }
    if !filters.genres.isEmpty { parts.append(filters.genres.joined(separator: ", ")) }
    if let artist = filters.artist { parts.append(artist.name) }
    return parts.isEmpty ? nil : parts.joined(separator: " · ")
}

private struct NoResultsStateView: View {
    let filters: FilterState
    let query: String
    let onClearAllFilters: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Nenhum evento encontrado com esses filtros")
                .font(.headline)
            if let summary = describeActiveQuery(filters: filters, query: query) {
                Text(summary)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            Button("Limpar filtros", action: onClearAllFilters)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct ResultsErrorStateView: View {
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Não foi possível carregar os resultados")
                .font(.headline)
            Button("Tentar novamente", action: onRetry)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
