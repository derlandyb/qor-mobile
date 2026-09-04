import SwiftUI
import MapKit
import shared

/// Errors surfaced by [EventDetailViewModel]'s continuation-wrapped bridge over `shared`'s
/// completion-handler-style suspend export — used only when a Kotlin call reports failure
/// without an `NSError`, which the generated interop doesn't do in practice but the Swift type
/// system still requires a non-optional `Error` to throw.
enum EventDetailError: Error {
    case unknown
}

/// I13 — loader + geocoding state for [EventDetailView] (mirrors Android's `EventDetailViewModel`
/// + A22's client-side geocoding retrofit, DISC-07–DISC-13). `fetchDetail`/`geocode` are
/// injectable seams (default: real `GetEventDetails`/`CLGeocoder` calls) — `shared`'s
/// `GetEventDetails` class can't be subclassed/faked from Swift (no `open` modifier), so the
/// seam lives here instead, at the boundary this class owns.
@MainActor
final class EventDetailViewModel: ObservableObject {
    enum LoadState: Equatable {
        case loading
        case error
        case content(EventDetail)

        static func == (lhs: LoadState, rhs: LoadState) -> Bool {
            switch (lhs, rhs) {
            case (.loading, .loading), (.error, .error):
                return true
            case let (.content(lhsDetail), .content(rhsDetail)):
                return lhsDetail === rhsDetail
            default:
                return false
            }
        }
    }

    @Published private(set) var loadState: LoadState = .loading
    @Published private(set) var mapState: EventMapState = .loading

    private let fetchDetail: (String) async throws -> EventDetail
    private let geocode: (String) async -> [GeoPoint]?

    init(
        fetchDetail: @escaping (String) async throws -> EventDetail = EventDetailViewModel.liveFetchDetail,
        geocode: @escaping (String) async -> [GeoPoint]? = EventDetailViewModel.liveGeocode
    ) {
        self.fetchDetail = fetchDetail
        self.geocode = geocode
    }

    /// Fetches [eventId]'s detail, then — only for [EventDetail.Active] (DISC-10/A22) — geocodes
    /// its address into [mapState]. Mirrors Android's `EventDetailViewModel.load` + the map's own
    /// `LaunchedEffect(event.address)`, sequenced here instead since SwiftUI has no direct
    /// equivalent for "run once content resolves to Active".
    func load(eventId: String) async {
        loadState = .loading
        mapState = .loading
        do {
            let detail = try await fetchDetail(eventId)
            loadState = .content(detail)
            if let active = detail as? EventDetail.Active {
                mapState = toEventMapState(await geocode(active.event.address))
            }
        } catch {
            loadState = .error
        }
    }

    static func liveFetchDetail(_ eventId: String) async throws -> EventDetail {
        try await withCheckedThrowingContinuation { continuation in
            IosDependencies.shared.getEventDetails().execute(eventId: eventId) { detail, error in
                if let detail {
                    continuation.resume(returning: detail)
                } else {
                    continuation.resume(throwing: error ?? EventDetailError.unknown)
                }
            }
        }
    }

    /// Geocodes off the main thread naturally (`CLGeocoder`'s async API is not main-thread-bound
    /// the way Android's synchronous `Geocoder` overload needed `Dispatchers.IO`). Returns `nil`
    /// on any failure so `toEventMapState` maps it to `.failed` — never crashes (DISC-10's
    /// graceful-degradation contract, mirroring Android's A22 retrofit).
    static func liveGeocode(_ address: String) async -> [GeoPoint]? {
        do {
            let placemarks = try await CLGeocoder().geocodeAddressString(address)
            return placemarks.compactMap { placemark in
                guard let coordinate = placemark.location?.coordinate else { return nil }
                return GeoPoint(latitude: coordinate.latitude, longitude: coordinate.longitude)
            }
        } catch {
            return nil
        }
    }
}

private let eventMapHeight: CGFloat = 200
private let eventMapSpanDegrees: CLLocationDegrees = 0.01

/// I13 — event detail (DISC-07–DISC-13), mirroring Android's `EventDetailScreen` + A22's map
/// retrofit. Branches over [EventDetail]'s three subtypes: [EventDetail.Cancelled]/
/// [EventDetail.Ended] render a status banner only (DISC-07), [EventDetail.Active] renders the full
/// content — description, date/time, address, genre, free/paid indicator, a ticket-link button
/// (paid only, DISC-08/09), an embedded MapKit map (DISC-10/A22 parity) with an "Abrir no mapa"
/// fallback, a per-promoter contact list (DISC-11), and a native share action (DISC-12) via
/// `ShareLink`.
///
/// **DISC-13 (accessibility info/event rules/notes) is skipped** — neither `Event` nor
/// `EventDetail` exposes such a field, same gap Android's `EventDetailScreen` documents.
///
/// This view never pushes navigation itself (I14's job) — [onBack] and [onOpenURL] are the seams
/// I14 wires: [onBack] for a back affordance, [onOpenURL] for every external-URL action (ticket
/// link, "Abrir no mapa" fallback, and promoter phone/email/Instagram/TikTok links), defaulting
/// to `UIApplication.shared.open(_:)` so the screen is usable stand-alone before I14 lands.
struct EventDetailView: View {
    let eventId: String
    var onBack: () -> Void
    var onOpenURL: (URL) -> Void

    @StateObject private var viewModel: EventDetailViewModel

    @MainActor
    init(
        eventId: String,
        onBack: @escaping () -> Void = {},
        onOpenURL: @escaping (URL) -> Void = { url in UIApplication.shared.open(url) },
        viewModel: EventDetailViewModel? = nil
    ) {
        self.eventId = eventId
        self.onBack = onBack
        self.onOpenURL = onOpenURL
        _viewModel = StateObject(wrappedValue: viewModel ?? EventDetailViewModel())
    }

    var body: some View {
        ZStack {
            QorColor.bgDeep.ignoresSafeArea()

            switch viewModel.loadState {
            case .loading:
                ProgressView()
                    .tint(QorColor.accentPink)
            case .error:
                errorView
            case .content(let detail):
                contentView(for: detail)
            }
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .foregroundStyle(QorColor.textPrimary)
                }
                .accessibilityIdentifier("event_detail_back")
            }
        }
        .task(id: eventId) {
            await viewModel.load(eventId: eventId)
        }
    }

    @ViewBuilder
    private func contentView(for detail: EventDetail) -> some View {
        if let active = detail as? EventDetail.Active {
            ScrollView {
                activeContent(active)
                    .padding(QorSpace.space4)
            }
        } else if detail is EventDetail.Cancelled {
            statusBanner(String(localized: "event_detail_status_cancelled"))
        } else if detail is EventDetail.Ended {
            statusBanner(String(localized: "event_detail_status_ended"))
        }
    }

    @ViewBuilder
    private func activeContent(_ detail: EventDetail.Active) -> some View {
        let event = detail.event
        let dateLabel = formatDateBadge(isoStartsAt: event.startsAt)
        let timeLabel = formatEventTime(isoStartsAt: event.startsAt)
        let shareText = String(format: String(localized: "event_detail_share_text"), event.title, event.address)

        VStack(alignment: .leading, spacing: QorSpace.space4) {
            PlaceholderImage()
                .frame(height: eventMapHeight)

            eventHeader(event: event, dateLabel: dateLabel, timeLabel: timeLabel)

            mapSection(address: event.address)

            Text(event.description)
                .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)

            if !event.isFree, let ticketUrlString = event.ticketUrl, let ticketUrl = URL(string: ticketUrlString) {
                PrimaryButton(
                    text: String(localized: "cta_comprar_ingresso"),
                    onClick: { onOpenURL(ticketUrl) }
                )
                .accessibilityIdentifier("event_detail_ticket_button")
            }

            if !detail.promoters.isEmpty {
                Text(String(localized: "event_detail_contacts_title"))
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextEventTitle.shared.SizeSp), weight: .bold))
                    .foregroundStyle(QorColor.textPrimary)

                ForEach(Array(detail.promoters.enumerated()), id: \.offset) { _, contact in
                    PromoterContactRow(contact: contact, onOpenURL: onOpenURL)
                }
            }

            shareLink(text: shareText)
        }
    }

    @ViewBuilder
    private func eventHeader(event: Event, dateLabel: DateBadgeLabel, timeLabel: String) -> some View {
        Text(event.title)
            .font(.system(size: CGFloat(QualORockThemeTokens.TextEventTitleLg.shared.SizeSp), weight: .bold))
            .foregroundStyle(QorColor.textPrimary)
            .accessibilityIdentifier("event_detail_title")

        HStack(spacing: QorSpace.space2) {
            GenreTag(genre: event.genre)
            Text(String(localized: event.isFree ? "event_detail_label_free" : "event_detail_label_paid"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextBadge.shared.SizeSp), weight: .semibold))
                .foregroundStyle(QorColor.textSecondary)
        }

        Text("\(dateLabel.day) \(dateLabel.month) · \(timeLabel)")
            .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
            .foregroundStyle(QorColor.textSecondary)

        Text(event.address)
            .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp), weight: .semibold))
            .foregroundStyle(QorColor.textPrimary)
            .accessibilityIdentifier("event_detail_address")
    }

    @ViewBuilder
    private func shareLink(text: String) -> some View {
        let borderWidth = CGFloat(QualORockThemeTokens.shared.BorderWidthHairlineDp)
        ShareLink(item: text) {
            Text(String(localized: "cta_compartilhar"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextButton.shared.SizeSp), weight: .semibold))
                .foregroundStyle(QorColor.textPrimary)
                .frame(maxWidth: .infinity, minHeight: 44)
                .overlay(
                    RoundedRectangle(cornerRadius: QorRadius.radiusMd)
                        .stroke(QorColor.textPrimary, lineWidth: borderWidth)
                )
        }
        .accessibilityIdentifier("event_detail_share")
    }

    /// DISC-10/A22 parity — a real embedded MapKit map once [EventMapState.located] resolves;
    /// while `.loading`/`.failed`, an "Abrir no mapa" fallback link opens the address in Maps
    /// instead of a blank/broken map (never crashes), matching Android's fallback philosophy.
    @ViewBuilder
    private func mapSection(address: String) -> some View {
        switch viewModel.mapState {
        case .located(let point):
            let coordinate = CLLocationCoordinate2D(latitude: point.latitude, longitude: point.longitude)
            Map(
                coordinateRegion: .constant(
                    MKCoordinateRegion(
                        center: coordinate,
                        span: MKCoordinateSpan(latitudeDelta: eventMapSpanDegrees, longitudeDelta: eventMapSpanDegrees)
                    )
                ),
                annotationItems: [EventMapPin(coordinate: coordinate)]
            ) { pin in
                MapMarker(coordinate: pin.coordinate)
            }
            .frame(height: eventMapHeight)
            .clipShape(RoundedRectangle(cornerRadius: QorRadius.radiusMd))
            .accessibilityIdentifier("event_detail_map")
        case .loading, .failed:
            SecondaryButton(
                text: String(localized: "cta_abrir_no_mapa"),
                onClick: { onOpenURL(mapFallbackURL(for: address)) }
            )
            .accessibilityIdentifier("event_detail_map_fallback")
        }
    }

    private func mapFallbackURL(for address: String) -> URL {
        let encoded = address.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? address
        return URL(string: "https://maps.apple.com/?q=\(encoded)") ?? URL(string: "https://maps.apple.com")!
    }

    private var errorView: some View {
        VStack(spacing: QorSpace.space3) {
            Text(String(localized: "event_detail_error_message"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)
                .multilineTextAlignment(.center)

            PrimaryButton(
                text: String(localized: "event_detail_cta_tentar_novamente"),
                onClick: { Task { await viewModel.load(eventId: eventId) } }
            )
        }
        .padding(QorSpace.space6)
    }

    private func statusBanner(_ message: String) -> some View {
        Text(message)
            .font(.system(size: CGFloat(QualORockThemeTokens.TextEventTitle.shared.SizeSp), weight: .semibold))
            .foregroundStyle(QorColor.textSecondary)
            .multilineTextAlignment(.center)
            .padding(QorSpace.space6)
    }
}

private struct EventMapPin: Identifiable {
    let id = UUID()
    let coordinate: CLLocationCoordinate2D
}

/// DISC-11 — one tagged promoter's contact row. Each button is rendered only if its field is
/// present on [contact] (omitted, never shown blank), mirroring Android's `PromoterContactRow`.
private struct PromoterContactRow: View {
    let contact: EventPromoterContact
    let onOpenURL: (URL) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: QorSpace.space2) {
            Text(contact.name)
                .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp), weight: .semibold))
                .foregroundStyle(QorColor.textPrimary)

            HStack(spacing: QorSpace.space2) {
                if let phone = contact.phone, let url = URL(string: "tel:\(phone)") {
                    SecondaryButton(text: String(localized: "cta_ligar"), onClick: { onOpenURL(url) })
                }
                if let email = contact.email, let url = URL(string: "mailto:\(email)") {
                    SecondaryButton(text: String(localized: "cta_enviar_email"), onClick: { onOpenURL(url) })
                }
                if let handle = contact.instagram, let url = instagramURL(for: handle) {
                    InstagramCta(onClick: { onOpenURL(url) })
                }
                if let handle = contact.tiktok, let url = tiktokURL(for: handle) {
                    SecondaryButton(text: String(localized: "cta_tiktok"), onClick: { onOpenURL(url) })
                }
            }
        }
    }

    /// `EventPromoterContact.instagram`/`.tiktok` are raw handles per `api.md` T24 (no dedicated
    /// profile-URL field), so the web-profile URL is built client-side — a leading "@" is
    /// stripped defensively in case the API ever sends one, mirroring Android's own helper.
    private func instagramURL(for handle: String) -> URL? {
        URL(string: "https://instagram.com/\(handle.trimmingPrefix("@"))")
    }

    private func tiktokURL(for handle: String) -> URL? {
        URL(string: "https://www.tiktok.com/@\(handle.trimmingPrefix("@"))")
    }
}

private extension String {
    func trimmingPrefix(_ prefix: String) -> String {
        hasPrefix(prefix) ? String(dropFirst(prefix.count)) : self
    }
}
