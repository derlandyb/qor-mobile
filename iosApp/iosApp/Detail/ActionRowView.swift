import Shared
import SwiftUI

/// Save/Share only — DETAIL-002/DETAIL-005's two equal-weight primary actions. The ticket link
/// renders in `EventDetailView`'s hero, next to the date/price row, per event-details/design.md's
/// Gap 1 correction.
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

            Spacer()
        }
        .padding(.horizontal)
    }
}
