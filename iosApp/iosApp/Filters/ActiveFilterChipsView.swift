import Shared
import SwiftUI

private func label(for chip: FilterChip) -> String {
    switch chip {
    case let date as FilterChipDateChip: return date.bucket.label
    case let city as FilterChipCityChip: return city.city
    case let genre as FilterChipGenreChip: return genre.genres.joined(separator: ", ")
    case let artist as FilterChipArtistChip: return artist.artist.name
    default: return ""
    }
}

struct ActiveFilterChipsView: View {
    let chips: [FilterChip]
    let onRemove: (FilterChip) -> Void
    let onClearAll: () -> Void

    var body: some View {
        if !chips.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(chips.enumerated()), id: \.offset) { _, chip in
                        Button {
                            onRemove(chip)
                        } label: {
                            HStack(spacing: 4) {
                                Text(label(for: chip))
                                Image(systemName: "xmark")
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color(.systemGray5))
                            .clipShape(Capsule())
                        }
                        .accessibilityLabel("Remover filtro \(label(for: chip))")
                    }
                    Button("Limpar filtros", action: onClearAll)
                }
                .padding(.horizontal)
            }
        }
    }
}
