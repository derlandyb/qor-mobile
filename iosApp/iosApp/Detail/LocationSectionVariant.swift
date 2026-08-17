import Shared

/// DETAIL-004's three-way rule as a pure, testable decision — static map, address-only, or omitted entirely.
enum LocationSectionVariant: Equatable {
    case map(url: String, address: String?)
    case addressOnly(String)
    case omitted

    static func from(venue: Venue) -> LocationSectionVariant {
        if let mapUrl = venue.staticMapUrl {
            return .map(url: mapUrl, address: venue.address)
        }
        if let address = venue.address {
            return .addressOnly(address)
        }
        return .omitted
    }
}
