import SwiftUI
import shared

private let entranceStartScale = 0.96
private let entranceStartOffsetY: CGFloat = 16

/// `card-enter`'s (design-system.md §3) per-index stagger delay — pure, so it's unit-testable
/// without SwiftUI. Mirrors Android's `entranceStaggerDelayMillis`.
func entranceStaggerDelaySeconds(index: Int) -> Double {
    QorMotion.durationStagger * Double(index)
}

/// I2 — `card-enter`: fade + rise + scale, staggered by [entranceStaggerDelaySeconds] per index.
/// Apply to each `LazyVGrid`/`List` item's view (consumed by later screen tasks).
private struct EntranceStaggerModifier: ViewModifier {
    let index: Int
    @State private var appeared = false

    func body(content: Content) -> some View {
        content
            .opacity(appeared ? 1 : 0)
            .offset(y: appeared ? 0 : entranceStartOffsetY)
            .scaleEffect(appeared ? 1 : entranceStartScale)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + entranceStaggerDelaySeconds(index: index)) {
                    withAnimation(.timingCurve(
                        Double(QualORockThemeTokens.shared.EaseBeat.x1),
                        Double(QualORockThemeTokens.shared.EaseBeat.y1),
                        Double(QualORockThemeTokens.shared.EaseBeat.x2),
                        Double(QualORockThemeTokens.shared.EaseBeat.y2),
                        duration: QorMotion.durationSlow
                    )) {
                        appeared = true
                    }
                }
            }
    }
}

extension View {
    func entranceStagger(index: Int) -> some View {
        modifier(EntranceStaggerModifier(index: index))
    }
}
