import SwiftUI
import shared

/// I3 — MVP Core's bottom-nav destinations, mirrors Android's `BottomNavDestination`.
/// `.favoritos` renders as a disabled stub per mobile.md's I3 scope note: the favoriting action
/// itself is Milestone 2 (Social & Notifications) — the tab exists here only for
/// nav-shell completeness.
enum BottomNavDestination: CaseIterable {
    case inicio
    case explorar
    case favoritos
    case perfil

    var labelKey: String.LocalizationValue {
        switch self {
        case .inicio: return "nav_inicio"
        case .explorar: return "nav_explorar"
        case .favoritos: return "nav_favoritos"
        case .perfil: return "nav_perfil"
        }
    }

    var enabled: Bool { self != .favoritos }
}

private let underlineHeight: CGFloat = 2

/// I3 — fixed bottom navigation, design-system.md active-state label + accent-color underline.
struct BottomNav: View {
    let current: BottomNavDestination
    let onSelect: (BottomNavDestination) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(BottomNavDestination.allCases, id: \.self) { destination in
                BottomNavItem(destination: destination, isSelected: destination == current, onSelect: onSelect)
            }
        }
        .background(QorColor.surfaceCard)
    }
}

private struct BottomNavItem: View {
    let destination: BottomNavDestination
    let isSelected: Bool
    let onSelect: (BottomNavDestination) -> Void

    var body: some View {
        let textColor: Color = {
            if !destination.enabled { return QorColor.textTertiary }
            return isSelected ? QorColor.accentPink : QorColor.textSecondary
        }()

        Button {
            onSelect(destination)
        } label: {
            VStack(spacing: 0) {
                Text(String(localized: destination.labelKey))
                    .font(.system(
                        size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp),
                        weight: isSelected ? .semibold : .regular
                    ))
                    .foregroundStyle(textColor)
                Rectangle()
                    .fill(isSelected ? QorColor.accentPink : .clear)
                    .frame(height: underlineHeight)
            }
            .padding(.vertical, QorSpace.space2)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
        .disabled(!destination.enabled)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}
