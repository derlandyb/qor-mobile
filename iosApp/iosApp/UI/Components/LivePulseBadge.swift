import SwiftUI
import shared

private let pulseDurationSeconds = 1.8
private let pulseMinOpacity = 0.4
private let dotSize: CGFloat = 8

/// I2 — "ao vivo agora" live-pulse dot: the one continuous-loop animation in the system
/// (design-system.md §3). Mirrors Android's `LivePulseBadge`.
struct LivePulseBadge: View {
    @State private var pulsing = false

    var body: some View {
        HStack(spacing: QorSpace.space1) {
            Circle()
                .fill(QorColor.accentPink)
                .frame(width: dotSize, height: dotSize)
                .opacity(pulsing ? pulseMinOpacity : 1)
                .animation(
                    .easeInOut(duration: pulseDurationSeconds).repeatForever(autoreverses: true),
                    value: pulsing
                )
            Text(String(localized: "live_pulse_label"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextBadge.shared.SizeSp), weight: .semibold))
                .foregroundStyle(QorColor.accentPink)
        }
        .padding(.horizontal, QorSpace.space2)
        .padding(.vertical, QorSpace.space1)
        .background(QorColor.accentPink.opacity(0.15))
        .clipShape(Capsule())
        .accessibilityLabel(String(localized: "content_description_live_now"))
        .onAppear { pulsing = true }
    }
}
