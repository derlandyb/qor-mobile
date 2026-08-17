import Shared
import SwiftUI

struct ActionRowView: View {
    let event: Event
    let baseUrl: String
    let onShared: () -> Void

    @State private var saved = false

    var body: some View {
        HStack(spacing: 16) {
            Button {
                saved.toggle()
            } label: {
                Image(systemName: saved ? "heart.fill" : "heart")
                    .padding(16)
                    .background(Circle().fill(Color.accentColor))
                    .foregroundColor(.white)
            }

            Button {
                UIPasteboard.general.string = ShareLinkBuilder.canonicalURL(forEventId: event.id, baseUrl: baseUrl)
                onShared()
            } label: {
                Image(systemName: "square.and.arrow.up")
                    .padding(16)
                    .background(Circle().fill(Color.accentColor))
                    .foregroundColor(.white)
            }

            if let ticketUrlString = event.ticketUrl, let ticketUrl = URL(string: ticketUrlString) {
                Link("Comprar Ingresso", destination: ticketUrl)
                    .font(.footnote)
            }

            Spacer()
        }
        .padding(.horizontal)
    }
}
