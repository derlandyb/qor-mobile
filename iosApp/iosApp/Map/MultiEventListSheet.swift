import Shared
import SwiftUI

/// Shown when a cluster marker is tapped — whether it groups multiple events at the same venue, or several
/// nearby venues (MAP-004/010). Both cases resolve to the same list-on-tap: every constituent `Event`, each
/// tappable to its own `MarkerPreviewSheet` — no two-step zoom-then-list.
struct MultiEventListSheet: View {
    let events: [Event]
    let onSelect: (Event) -> Void

    var body: some View {
        List(events, id: \.id) { event in
            Button {
                onSelect(event)
            } label: {
                EventCardView(event: event)
            }
            .buttonStyle(.plain)
        }
        .listStyle(.plain)
    }
}
