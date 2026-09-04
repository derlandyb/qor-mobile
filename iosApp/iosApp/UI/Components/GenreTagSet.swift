import SwiftUI
import shared

/// design-system.md §4.3 tint/text color for one genre chip; `solidBackground` = Sertanejo's
/// solid treatment.
struct GenreTagStyle {
    let backgroundColor: Color
    let textColor: Color
}

private let tintAlpha = 0.15

/// I2 — the 5-genre color map from design-system.md §4.3 (mirrors Android's `GenreTagColors`).
/// `Event.genre` is a raw API string (no genre-list endpoint yet, same gap documented in
/// `qor-website`/Android's own `GenreTagSet`), so an unknown genre falls back to a neutral tint.
enum GenreTagColors {
    static func style(for genre: String) -> GenreTagStyle {
        switch genre.lowercased() {
        case "rock":
            return tinted(QorColor.accentOrange)
        case "samba":
            return tinted(QorColor.accentPink)
        case "sertanejo":
            return GenreTagStyle(backgroundColor: QorColor.accentPink, textColor: QorColor.bgBase)
        case "eletrônico", "eletronico":
            return tinted(QorColor.accentPurple)
        case "reggae":
            return tinted(QorColor.accentBlue)
        default:
            return GenreTagStyle(backgroundColor: QorColor.surfaceCardHover, textColor: QorColor.textSecondary)
        }
    }

    private static func tinted(_ accent: Color) -> GenreTagStyle {
        GenreTagStyle(backgroundColor: accent.opacity(tintAlpha), textColor: accent)
    }
}

struct GenreTag: View {
    let genre: String

    var body: some View {
        let style = GenreTagColors.style(for: genre)
        Text(genre.uppercased())
            .font(.system(size: CGFloat(QualORockThemeTokens.TextBadge.shared.SizeSp), weight: .semibold))
            .foregroundStyle(style.textColor)
            .padding(.horizontal, QorSpace.space2)
            .padding(.vertical, QorSpace.space1)
            .background(style.backgroundColor)
            .clipShape(RoundedRectangle(cornerRadius: QorRadius.radiusSm))
            .accessibilityLabel(String(format: String(localized: "content_description_genre"), genre))
    }
}
