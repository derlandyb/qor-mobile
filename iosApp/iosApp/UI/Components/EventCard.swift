import SwiftUI
import shared

/// design-system.md §3's card-hover spec (`hover:scale-[1.03] hover:-translate-y-1`), as pure
/// functions of press state (mirrors Android's `EventCardMotion`).
enum EventCardMotion {
    // design-system.md §4.1's cover-image aspect ratio (4:5, portrait poster crop).
    static let imageAspectRatio: CGFloat = 4.0 / 5.0

    private static let restingScale = 1.0
    private static let pressedScale = 1.03
    private static let pressedRiseY: CGFloat = -4

    static func pressScale(pressed: Bool) -> Double { pressed ? pressedScale : restingScale }
    static func pressRiseY(pressed: Bool) -> CGFloat { pressed ? pressedRiseY : 0 }
}

/// I2 — Event Card (design-system.md §4.1). `onMapClick` opens a maps deep link built from
/// `Event.address` (no dedicated `mapsUrl` field on the API contract, mirrors Android's A2); an
/// Instagram CTA is omitted — `Event` has no `instagramUrl` field (the same documented gap as
/// `qor-website`/Android's own `EventCard`).
struct EventCard: View {
    let event: Event
    let onClick: () -> Void
    let onMapClick: () -> Void

    @State private var pressed = false

    var body: some View {
        let dateLabel = formatDateBadge(isoStartsAt: event.startsAt)
        let timeLabel = formatEventTime(isoStartsAt: event.startsAt)
        let cityStyle = CityFilterColors.style(for: event.city)

        VStack(spacing: 0) {
            Button(action: onClick) {
                VStack(alignment: .leading, spacing: 0) {
                    imageHolder(dateLabel: dateLabel)
                    contentBlock(timeLabel: timeLabel, cityStyle: cityStyle)
                }
            }
            .buttonStyle(.plain)

            MapaCta(onClick: onMapClick)
                .padding(.horizontal, QorSpace.space3)
                .padding(.bottom, QorSpace.space3)
        }
        .background(QorColor.surfaceCard)
        .clipShape(RoundedRectangle(cornerRadius: QorRadius.radiusLg))
        .overlay(
            RoundedRectangle(cornerRadius: QorRadius.radiusLg)
                .stroke(QorColor.borderSubtle, lineWidth: CGFloat(QualORockThemeTokens.shared.BorderWidthHairlineDp))
        )
        .scaleEffect(EventCardMotion.pressScale(pressed: pressed))
        .offset(y: EventCardMotion.pressRiseY(pressed: pressed))
        .animation(QorMotion.easeBeat, value: pressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in pressed = true }
                .onEnded { _ in pressed = false }
        )
    }

    @ViewBuilder
    private func imageHolder(dateLabel: DateBadgeLabel) -> some View {
        ZStack(alignment: .topLeading) {
            QorColor.bgBase
                .aspectRatio(EventCardMotion.imageAspectRatio, contentMode: .fill)

            VStack(spacing: 0) {
                Text(dateLabel.month)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(QorColor.textSecondary)
                Text(dateLabel.day)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(QorColor.textPrimary)
            }
            .padding(.horizontal, QorSpace.space2)
            .padding(.vertical, QorSpace.space1)
            .background(QorColor.bgDeep.opacity(0.8))
            .clipShape(RoundedRectangle(cornerRadius: QorRadius.radiusSm))
            .padding(QorSpace.space3)

            HStack {
                Spacer()
                GenreTag(genre: event.genre)
            }
            .padding(QorSpace.space3)
        }
        .clipShape(RoundedRectangle(cornerRadius: QorRadius.image))
    }

    @ViewBuilder
    private func contentBlock(timeLabel: String, cityStyle: CityFilterStyle) -> some View {
        VStack(alignment: .leading, spacing: QorSpace.space2) {
            Text(event.title)
                .font(.system(size: CGFloat(QualORockThemeTokens.TextEventTitle.shared.SizeSp), weight: .bold))
                .foregroundStyle(QorColor.textPrimary)
                .lineLimit(2)

            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(event.address)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(QorColor.textPrimary)
                    Text(timeLabel)
                        .font(.system(size: 13))
                        .foregroundStyle(QorColor.textSecondary)
                }
                Spacer()
                Text(String(localized: cityStyle.labelKey))
                    .textCase(.uppercase)
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextBadge.shared.SizeSp), weight: .semibold))
                    .foregroundStyle(cityStyle.activeColor)
                    .padding(.horizontal, QorSpace.space2)
                    .padding(.vertical, QorSpace.space1)
                    .background(cityStyle.activeColor.opacity(0.15))
                    .clipShape(Capsule())
            }
        }
        .padding(QorSpace.space3)
    }
}
