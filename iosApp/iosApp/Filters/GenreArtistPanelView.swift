import Shared
import SwiftUI

struct GenreArtistPanelView: View {
    let genreOptions: OptionsUiState
    let artistOptions: OptionsUiState
    let selectedGenres: Set<String>
    let selectedArtist: ArtistOption?
    let onToggleGenre: (String) -> Void
    let onSelectArtist: (ArtistOption?) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Filtros Avançados").font(.headline)

            section(title: "Gênero", options: genreOptions) { (options: [String]) in
                pillRow(options, id: \.self) { genre in
                    pill(label: genre, selected: selectedGenres.contains(genre)) {
                        onToggleGenre(genre)
                    }
                }
            }

            section(title: "Artista", options: artistOptions) { (options: [ArtistOption]) in
                pillRow(options, id: \.id) { artist in
                    pill(label: artist.name, selected: selectedArtist?.id == artist.id) {
                        onSelectArtist(selectedArtist?.id == artist.id ? nil : artist)
                    }
                }
            }
        }
        .padding(.horizontal)
    }

    @ViewBuilder
    private func section<T>(
        title: String,
        options: OptionsUiState,
        @ViewBuilder content: @escaping ([T]) -> some View
    ) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.subheadline).foregroundColor(.secondary)
            switch options {
            case is OptionsUiStateLoading:
                ProgressView()
            case let error as OptionsUiStateError:
                Text(error.message).font(.caption)
            case let loaded as OptionsUiStateLoaded<AnyObject>:
                let typed = (loaded.options as? [T]) ?? []
                if typed.isEmpty {
                    Text("Nada para filtrar ainda").font(.caption).foregroundColor(.secondary)
                } else {
                    content(typed)
                }
            default:
                EmptyView()
            }
        }
    }

    private func pillRow<T, ID: Hashable>(
        _ options: [T],
        id: KeyPath<T, ID>,
        @ViewBuilder pill: @escaping (T) -> some View
    ) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(options, id: id) { option in pill(option) }
            }
        }
    }

    private func pill(label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(selected ? Color.accentColor : Color(.systemGray5))
                .foregroundColor(selected ? .white : .primary)
                .clipShape(Capsule())
        }
    }
}
