import SwiftUI
import shared

/// I4 — design-system-consistent placeholder for an event with no `coverImageUrl`
/// (`Event.coverImageUrl == nil`).
struct PlaceholderImage: View {
    var body: some View {
        Rectangle()
            .fill(QorColor.bgBase)
            .clipShape(RoundedRectangle(cornerRadius: QorRadius.image))
            .accessibilityLabel(String(localized: "content_description_no_flyer"))
    }
}
