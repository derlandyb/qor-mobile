import Shared
import SwiftUI

struct LocationSectionView: View {
    let venue: Venue

    var body: some View {
        switch LocationSectionVariant.from(venue: venue) {
        case let .map(mapUrlString, address):
            VStack(alignment: .leading, spacing: 8) {
                Text("Local").font(.headline)
                if let mapUrl = URL(string: mapUrlString) {
                    AsyncImage(url: mapUrl) { image in
                        image.resizable().aspectRatio(16.0 / 7.0, contentMode: .fill)
                    } placeholder: {
                        Rectangle().fill(Color(.systemGray5)).aspectRatio(16.0 / 7.0, contentMode: .fit)
                    }
                    .frame(maxWidth: .infinity)
                    .aspectRatio(16.0 / 7.0, contentMode: .fill)
                    .clipped()
                    .cornerRadius(16)
                }
                if let address {
                    Text(address).font(.subheadline).foregroundColor(.secondary)
                }
                if let lat = venue.latitude?.doubleValue, let lng = venue.longitude?.doubleValue,
                   let mapsUrl = URL(string: "https://maps.google.com/?q=\(lat),\(lng)") {
                    Link("Ver no mapa", destination: mapsUrl)
                }
            }
            .padding()
        case let .addressOnly(address):
            VStack(alignment: .leading, spacing: 8) {
                Text("Local").font(.headline)
                Text(address).font(.subheadline).foregroundColor(.secondary)
            }
            .padding()
        case .omitted:
            EmptyView()
        }
    }
}
