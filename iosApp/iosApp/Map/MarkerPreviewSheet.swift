import Shared
import SwiftUI

/// Overlay preview shown when a single-event marker is tapped (MAP-002/009) — reuses `EventCardView`'s
/// styling, plus an explicit "open full detail" action. The map stays visible/interactive underneath.
struct MarkerPreviewSheet: View {
    let event: Event
    let onOpenDetail: (Event) -> Void

    var body: some View {
        VStack(spacing: 16) {
            EventCardView(event: event)
            Button("Ver detalhes do evento") { onOpenDetail(event) }
        }
        .padding()
    }
}
