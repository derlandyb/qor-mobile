import Foundation
import Shared
import SwiftUI

struct EventCardView: View {
    let event: Event

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ZStack(alignment: .topTrailing) {
                if let coverImageUrl = event.coverImageUrl, let url = URL(string: coverImageUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(16.0 / 9.0, contentMode: .fill)
                    } placeholder: {
                        Rectangle().fill(Color(red: 0.93, green: 0.96, blue: 0.98))
                            .aspectRatio(16.0 / 9.0, contentMode: .fit)
                    }
                    .frame(maxWidth: .infinity)
                    .aspectRatio(16.0 / 9.0, contentMode: .fill)
                    .clipped()
                    .cornerRadius(16)
                } else {
                    Rectangle()
                        .fill(Color(red: 0.93, green: 0.96, blue: 0.98))
                        .aspectRatio(16.0 / 9.0, contentMode: .fit)
                        .cornerRadius(16)
                }

                HStack(spacing: 12) {
                    Image(systemName: "heart")
                    Image(systemName: "square.and.arrow.up")
                }
                .padding(8)

                if event.status == EventStatus.cancelled {
                    Text("Cancelado")
                        .font(.caption)
                        .foregroundColor(.red)
                        .padding(6)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                }
            }

            Text(event.title)
                .font(.headline)
            Text(event.venue.name)
                .font(.subheadline)
                .foregroundColor(.secondary)

            if let label = priceLabel(event.price) {
                Text(label)
                    .font(.caption)
            }
        }
        .padding(.vertical, 4)
    }
}

private func priceLabel(_ price: Price?) -> String? {
    guard let price else { return nil }
    if price.isFree { return "Grátis" }
    guard let min = price.min?.doubleValue else { return nil }

    let formatter = NumberFormatter()
    formatter.numberStyle = .currency
    formatter.locale = Locale(identifier: "pt_BR")
    let formatted = formatter.string(from: NSNumber(value: min)) ?? String(min)
    return "A partir de \(formatted)"
}
