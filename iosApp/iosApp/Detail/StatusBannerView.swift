import Shared
import SwiftUI

struct StatusBannerView: View {
    let bannerStatus: BannerStatus?

    var body: some View {
        switch bannerStatus {
        case .cancelled:
            banner(text: "Evento cancelado", systemImage: "xmark.octagon.fill", background: .red, foreground: .white)
        case .finished:
            banner(
                text: "Este evento já aconteceu",
                systemImage: "calendar.badge.exclamationmark",
                background: Color(.systemGray5),
                foreground: .primary
            )
        case nil:
            EmptyView()
        @unknown default:
            EmptyView()
        }
    }

    private func banner(text: String, systemImage: String, background: Color, foreground: Color) -> some View {
        HStack {
            Image(systemName: systemImage)
            Text(text)
        }
        .foregroundColor(foreground)
        .frame(maxWidth: .infinity)
        .padding()
        .background(background)
    }
}
