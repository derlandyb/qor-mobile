import SwiftUI
import shared

/// I2 — bridges `shared`'s framework-agnostic `QualORockThemeTokens` (ARGB `Long` hex values,
/// see `shared/src/commonMain/kotlin/design/QualORockThemeTokens.kt`) into SwiftUI `Color`,
/// mirroring Android's own `Color(QualORockThemeTokens.ColorBgDeep)` conversion. No hex/px/ms
/// literal is re-declared here — every value traces back to the one shared source of truth.
extension Color {
    /// `argb` is an ARGB `Int64` as produced by Kotlin's `0xFFRRGGBB` hex literals.
    init(argb: Int64) {
        let alpha = Double((argb >> 24) & 0xFF) / 255.0
        let red = Double((argb >> 16) & 0xFF) / 255.0
        let green = Double((argb >> 8) & 0xFF) / 255.0
        let blue = Double(argb & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}

/// Typed accessors so component code reads `QorColor.bgDeep` instead of re-deriving the ARGB
/// conversion at every call site. Kotlin's Objective-C export keeps `QualORockThemeTokens`'
/// `const val` property names exactly as declared (PascalCase, not lowerCamelCase).
enum QorColor {
    static let bgDeep = Color(argb: QualORockThemeTokens.shared.ColorBgDeep)
    static let bgBase = Color(argb: QualORockThemeTokens.shared.ColorBgBase)
    static let surfaceCard = Color(argb: QualORockThemeTokens.shared.ColorSurfaceCard)
    static let surfaceCardHover = Color(argb: QualORockThemeTokens.shared.ColorSurfaceCardHover)
    static let borderSubtle = Color(argb: QualORockThemeTokens.shared.ColorBorderSubtle)
    static let textPrimary = Color(argb: QualORockThemeTokens.shared.ColorTextPrimary)
    static let textSecondary = Color(argb: QualORockThemeTokens.shared.ColorTextSecondary)
    static let textTertiary = Color(argb: QualORockThemeTokens.shared.ColorTextTertiary)

    static let accentPink = Color(argb: QualORockThemeTokens.shared.AccentPink)
    static let accentOrange = Color(argb: QualORockThemeTokens.shared.AccentOrange)
    static let accentPurple = Color(argb: QualORockThemeTokens.shared.AccentPurple)
    static let accentBlue = Color(argb: QualORockThemeTokens.shared.AccentBlue)

    static let danger = Color(argb: QualORockThemeTokens.shared.ColorDanger)
}

/// Spacing scale (dp/pt — 1:1 with the token's declared px value, per `design-system.md` §2.3).
enum QorSpace {
    static let space1 = CGFloat(QualORockThemeTokens.shared.Space1Dp)
    static let space2 = CGFloat(QualORockThemeTokens.shared.Space2Dp)
    static let space3 = CGFloat(QualORockThemeTokens.shared.Space3Dp)
    static let space4 = CGFloat(QualORockThemeTokens.shared.Space4Dp)
    static let space5 = CGFloat(QualORockThemeTokens.shared.Space5Dp)
    static let space6 = CGFloat(QualORockThemeTokens.shared.Space6Dp)
    static let space7 = CGFloat(QualORockThemeTokens.shared.Space7Dp)
}

enum QorRadius {
    static let radiusSm = CGFloat(QualORockThemeTokens.shared.RadiusSmDp)
    static let radiusMd = CGFloat(QualORockThemeTokens.shared.RadiusMdDp)
    static let radiusLg = CGFloat(QualORockThemeTokens.shared.RadiusLgDp)
    static let image = CGFloat(QualORockThemeTokens.shared.RadiusImageDp)
    static let pill = CGFloat(QualORockThemeTokens.shared.RadiusPillDp)
}

enum QorMotion {
    static let durationFast = Double(QualORockThemeTokens.shared.DurationFastMs) / 1000
    static let durationBase = Double(QualORockThemeTokens.shared.DurationBaseMs) / 1000
    static let durationSlow = Double(QualORockThemeTokens.shared.DurationSlowMs) / 1000
    static let durationStagger = Double(QualORockThemeTokens.shared.DurationStaggerMs) / 1000

    /// `--ease-beat` — hover/press scale, entrance pop (overshoot).
    static let easeBeat: Animation = {
        let easing = QualORockThemeTokens.shared.EaseBeat
        return .timingCurve(
            Double(easing.x1), Double(easing.y1), Double(easing.x2), Double(easing.y2), duration: durationBase
        )
    }()

    /// `--ease-smooth` — color/gradient shifts, opacity.
    static let easeSmooth: Animation = {
        let easing = QualORockThemeTokens.shared.EaseSmooth
        return .timingCurve(
            Double(easing.x1), Double(easing.y1), Double(easing.x2), Double(easing.y2), duration: durationBase
        )
    }()
}
