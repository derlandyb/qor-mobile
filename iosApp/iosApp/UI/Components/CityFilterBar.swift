import SwiftUI
import shared

/// design-system.md §4.2's per-hub badge treatment: active/inactive colors + the pt-BR hub label.
struct CityFilterStyle {
    let activeColor: Color
    let activeTextColor: Color
    let labelKey: String.LocalizationValue
}

private let inactiveTintAlpha = 0.15

/// I2 — the 4-hub color table from design-system.md §4.2, keyed by [City] (mirrors Android's
/// `CityFilterColors`).
enum CityFilterColors {
    static func style(for city: City) -> CityFilterStyle {
        switch city {
        case .vitoria:
            return CityFilterStyle(
                activeColor: QorColor.accentPink, activeTextColor: QorColor.bgDeep, labelKey: "city_vitoria"
            )
        case .vilavelha:
            return CityFilterStyle(
                activeColor: QorColor.accentBlue, activeTextColor: QorColor.bgDeep, labelKey: "city_vila_velha"
            )
        case .serra:
            return CityFilterStyle(
                activeColor: QorColor.accentOrange, activeTextColor: QorColor.bgDeep, labelKey: "city_serra"
            )
        case .cariacica:
            return CityFilterStyle(
                activeColor: QorColor.accentPurple, activeTextColor: .white, labelKey: "city_cariacica"
            )
        default:
            // Kotlin/Native's Objective-C export represents `City` as a class hierarchy, not a
            // true Swift enum, so `switch` requires this clause even though every real case is
            // covered above — fail loudly (mirrors the fail-loud, no-catch-all rule
            // `shared/domain/enum/City.kt` documents) rather than silently defaulting to a real
            // city's styling, which would misrepresent a genuine contract break as valid data.
            fatalError("Unhandled City case: \(city)")
        }
    }
}

/// I2 — Horizontal City Filter Bar (design-system.md §4.2).
struct CityFilterBar: View {
    let selected: City?
    let onSelect: (City) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: QorSpace.space2) {
                ForEach(City.entries, id: \.self) { city in
                    CityFilterPill(city: city, isActive: city == selected) { onSelect(city) }
                }
            }
            .padding(.horizontal, QorSpace.space3)
            .padding(.vertical, QorSpace.space2)
        }
    }
}

private struct CityFilterPill: View {
    let city: City
    let isActive: Bool
    let onClick: () -> Void

    var body: some View {
        let style = CityFilterColors.style(for: city)
        let backgroundColor = isActive ? style.activeColor : style.activeColor.opacity(inactiveTintAlpha)
        let textColor = isActive ? style.activeTextColor : style.activeColor

        Button(action: onClick) {
            Text(String(localized: style.labelKey))
                .textCase(.uppercase)
                .font(.system(size: CGFloat(QualORockThemeTokens.TextBadge.shared.SizeSp), weight: .semibold))
                .foregroundStyle(textColor)
                .padding(.horizontal, QorSpace.space3)
                .padding(.vertical, QorSpace.space2)
                .background(backgroundColor)
                .clipShape(Capsule())
                .scaleEffect(isActive ? 1.05 : 1.0)
                .animation(QorMotion.easeSmooth, value: isActive)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isActive ? [.isSelected] : [])
    }
}
