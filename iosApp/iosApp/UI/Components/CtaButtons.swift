import SwiftUI
import shared

/// design-system.md §4.4's two CTA treatments, as pure functions of press state so the target
/// animation values are unit-testable without SwiftUI (mirrors Android's `CtaButtonMotion`).
enum CtaButtonMotion {
    static let mapaRestingAlpha = 0.10

    /// "Ver no Mapa": 10%-tint outline at rest, fully solid blue on press.
    static func mapaBackgroundAlpha(pressed: Bool) -> Double { pressed ? 1 : mapaRestingAlpha }

    /// "Ver Instagram": `background-position` 0% (left) at rest, 100% (right) on press.
    static func instagramGradientOffset(pressed: Bool) -> Double { pressed ? 1 : 0 }
}

private let ctaButtonMinHeight: CGFloat = 44

/// I2 — "Ver no Mapa" (blue outline → solid on press), design-system.md §4.4.
struct MapaCta: View {
    let onClick: () -> Void
    @State private var pressed = false

    var body: some View {
        let backgroundAlpha = CtaButtonMotion.mapaBackgroundAlpha(pressed: pressed)
        Button(action: onClick) {
            Text(String(localized: "cta_ver_no_mapa"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextButton.shared.SizeSp), weight: .semibold))
                .foregroundStyle(pressed ? QorColor.bgDeep : QorColor.accentBlue)
                .frame(maxWidth: .infinity, minHeight: ctaButtonMinHeight)
                .background(QorColor.accentBlue.opacity(backgroundAlpha))
                .clipShape(RoundedRectangle(cornerRadius: QorRadius.radiusMd))
        }
        .buttonStyle(.plain)
        .animation(QorMotion.easeBeat, value: pressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in pressed = true }
                .onEnded { _ in pressed = false }
        )
        .accessibilityIdentifier("mapa_cta")
    }
}

/// I2 — "Ver Instagram" (pink→purple animated gradient), design-system.md §4.4.
struct InstagramCta: View {
    let onClick: () -> Void
    @State private var pressed = false

    var body: some View {
        Button(action: onClick) {
            Text(String(localized: "cta_ver_instagram"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextButton.shared.SizeSp), weight: .semibold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, minHeight: ctaButtonMinHeight)
                .background(
                    LinearGradient(
                        colors: [QorColor.accentPink, QorColor.accentPurple],
                        startPoint: pressed ? .trailing : .leading,
                        endPoint: pressed ? .leading : .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: QorRadius.radiusMd))
        }
        .buttonStyle(.plain)
        .animation(QorMotion.easeSmooth, value: pressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in pressed = true }
                .onEnded { _ in pressed = false }
        )
    }
}

/// Generic solid CTA for form submits (login, signup, etc.), design-system.md §4.4's filled-button
/// treatment applied with arbitrary text/action instead of `MapaCta`'s hardcoded copy. Shows a
/// spinner in place of the label while `isLoading`, inert (no tap, dimmed) while `!enabled`.
struct PrimaryButton: View {
    let text: String
    let onClick: () -> Void
    var enabled: Bool = true
    var isLoading: Bool = false

    var body: some View {
        Button(action: onClick) {
            Group {
                if isLoading {
                    ProgressView().tint(.white)
                } else {
                    Text(text)
                        .font(.system(size: CGFloat(QualORockThemeTokens.TextButton.shared.SizeSp), weight: .semibold))
                        .foregroundStyle(.white)
                }
            }
            .frame(maxWidth: .infinity, minHeight: ctaButtonMinHeight)
            .background(enabled ? QorColor.accentPink : QorColor.borderSubtle)
            .clipShape(RoundedRectangle(cornerRadius: QorRadius.radiusMd))
        }
        .buttonStyle(.plain)
        .disabled(!enabled || isLoading)
    }
}

/// Generic outline CTA (`MapaCta`'s resting treatment generalized), e.g. "Entrar com Google".
/// Inert (no tap, dimmed) while `!enabled` — used for disabled stubs where an integration
/// (Google Sign-In) doesn't exist yet.
struct SecondaryButton: View {
    let text: String
    let onClick: () -> Void
    var enabled: Bool = true

    var body: some View {
        let contentColor = enabled ? QorColor.textPrimary : QorColor.textTertiary
        Button(action: onClick) {
            Text(text)
                .font(.system(size: CGFloat(QualORockThemeTokens.TextButton.shared.SizeSp), weight: .semibold))
                .foregroundStyle(contentColor)
                .frame(maxWidth: .infinity, minHeight: ctaButtonMinHeight)
                .overlay(
                    RoundedRectangle(cornerRadius: QorRadius.radiusMd)
                        .stroke(contentColor, lineWidth: CGFloat(QualORockThemeTokens.shared.BorderWidthHairlineDp))
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}
