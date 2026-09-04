import SwiftUI
import shared

/// I4 — design-system-consistent empty state for event-discovery edge cases (empty list).
struct EmptyState: View {
    var message: String = String(localized: "empty_state_no_events")

    var body: some View {
        Text(message)
            .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp)))
            .foregroundStyle(QorColor.textSecondary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(QorSpace.space6)
    }
}
