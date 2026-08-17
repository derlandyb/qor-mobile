import Shared
import SwiftUI

struct EventCardView: View {
    let event: Event

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ZStack(alignment: .topTrailing) {
                Rectangle()
                    .fill(Color(red: 0.93, green: 0.96, blue: 0.98))
                    .aspectRatio(16.0 / 9.0, contentMode: .fit)
                    .cornerRadius(16)

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

            if let price = event.price {
                Text(price.isFree ? "Grátis" : "A partir de \(price.min?.doubleValue ?? 0)")
                    .font(.caption)
            }
        }
        .padding(.vertical, 4)
    }
}
