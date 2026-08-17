@testable import iosApp
import Shared
import XCTest

final class EventDetailTests: XCTestCase {

    private func makeVenue(staticMapUrl: String? = nil, address: String? = nil) -> Venue {
        Venue(
            id: "v1",
            name: "Matrix",
            imageUrl: nil,
            description: nil,
            city: "Vitória",
            address: address,
            latitude: nil,
            longitude: nil,
            staticMapUrl: staticMapUrl,
            contactPhone: nil,
            contactEmail: nil,
            socialLinks: nil,
            verificationStatus: VerificationStatus.verified
        )
    }

    func testGivenAVenueWithCoordinatesWhenResolvingTheLocationVariantThenTheMapVariantIsReturned() {
        let venue = makeVenue(staticMapUrl: "https://maps.googleapis.com/maps/api/staticmap?center=1,1", address: "Rua Rio Branco, 100")

        let variant = LocationSectionVariant.from(venue: venue)

        XCTAssertEqual(variant, .map(url: "https://maps.googleapis.com/maps/api/staticmap?center=1,1", address: "Rua Rio Branco, 100"))
    }

    func testGivenAVenueWithOnlyAnAddressWhenResolvingTheLocationVariantThenTheAddressOnlyVariantIsReturned() {
        let venue = makeVenue(staticMapUrl: nil, address: "Rua Rio Branco, 100")

        let variant = LocationSectionVariant.from(venue: venue)

        XCTAssertEqual(variant, .addressOnly("Rua Rio Branco, 100"))
    }

    func testGivenAVenueWithNeitherCoordinatesNorAddressWhenResolvingTheLocationVariantThenTheOmittedVariantIsReturned() {
        let venue = makeVenue(staticMapUrl: nil, address: nil)

        let variant = LocationSectionVariant.from(venue: venue)

        XCTAssertEqual(variant, .omitted)
    }

    func testGivenAnEventIdWhenBuildingTheCanonicalShareURLThenItPointsAtTheShareRoute() {
        let url = ShareLinkBuilder.canonicalURL(forEventId: "16", baseUrl: "http://localhost:8080")

        XCTAssertEqual(url, "http://localhost:8080/compartilhar/eventos/16")
    }

    @MainActor
    func testGivenAnInitialLoadFailureWhenTheDetailViewLoadsThenTheRetryStateIsReachable() {
        let wrapper = EventDetailViewModelWrapper(eventId: "1", baseUrl: "http://127.0.0.1:1")

        XCTAssertTrue(wrapper.state is EventDetailUiStateLoading)
        wrapper.retry()
    }
}
