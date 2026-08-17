import Shared
import SwiftUI

struct EventDetailView: View {
    @ObservedObject var viewModel: EventDetailViewModelWrapper
    let baseUrl: String

    var body: some View {
        Group {
            switch viewModel.state {
            case is EventDetailUiStateLoading:
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case is EventDetailUiStateNotFound:
                Text("Evento não encontrado")
                    .font(.headline)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case is EventDetailUiStateLoadError:
                VStack(spacing: 16) {
                    Text("Não foi possível carregar o evento")
                        .font(.headline)
                    Button("Tentar novamente") { viewModel.retry() }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            default:
                if let loaded = viewModel.state as? EventDetailUiStateLoaded {
                    loadedContent(event: loaded.event)
                }
            }
        }
    }

    private func loadedContent(event: Event) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                StatusBannerView(bannerStatus: event.bannerStatus)

                if let coverImageUrlString = event.coverImageUrl, let coverImageUrl = URL(string: coverImageUrlString) {
                    AsyncImage(url: coverImageUrl) { image in
                        image.resizable().aspectRatio(16.0 / 9.0, contentMode: .fill)
                    } placeholder: {
                        Rectangle().fill(Color(.systemGray5)).aspectRatio(16.0 / 9.0, contentMode: .fit)
                    }
                    .frame(maxWidth: .infinity)
                    .aspectRatio(16.0 / 9.0, contentMode: .fill)
                    .clipped()
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text(event.title).font(.title2).bold()
                    Text("\(event.venue.name) • \(event.city)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text(PriceLineFormatter.shared.format(price: event.price))
                        .font(.footnote)
                }
                .padding()

                ActionRowView(event: event, baseUrl: baseUrl, onShared: {})

                DescriptionSectionView(description: event.description)
                LocationSectionView(venue: event.venue)
                PromoterSectionView(promoter: event.promoter)
            }
        }
    }
}
