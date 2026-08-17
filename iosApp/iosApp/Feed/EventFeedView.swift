import Shared
import SwiftUI

struct EventFeedView: View {
    @ObservedObject var viewModel: EventFeedViewModelWrapper

    var body: some View {
        Group {
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
    }

    private var feedList: some View {
        List {
            ForEach(viewModel.state.groupedEvents, id: \.label) { group in
                Section(header: Text(group.label)) {
                    ForEach(group.events, id: \.id) { event in
                        EventCardView(event: event)
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
