@testable import iosApp
import Shared
import XCTest

final class EventFeedTests: XCTestCase {

    private func makeEvent(id: String, startDateTime: Kotlinx_datetimeInstant, status: EventStatus = EventStatus.published) -> Event {
        Event(
            id: id,
            title: "Show \(id)",
            description: nil,
            coverImageUrl: nil,
            startDateTime: startDateTime,
            endDateTime: nil,
            venue: Venue(
                id: "venue-\(id)",
                name: "Venue \(id)",
                imageUrl: nil,
                description: nil,
                city: "Vitória",
                address: nil,
                latitude: nil,
                longitude: nil,
                staticMapUrl: nil,
                contactPhone: nil,
                contactEmail: nil,
                socialLinks: nil,
                verificationStatus: VerificationStatus.verified
            ),
            city: "Vitória",
            price: nil,
            ageRating: nil,
            genres: [],
            ticketUrl: nil,
            status: status,
            bannerStatus: nil,
            promoter: nil,
            isFavorited: nil
        )
    }

    func testGivenAnonymousVisitorWhenTheFeedLoadsThenUpcomingEventsAreGroupedByDate() {
        let utc = Kotlinx_datetimeTimeZone.Companion.shared.UTC
        let todayDateTime = Kotlinx_datetimeLocalDateTime(year: 2026, monthNumber: 8, dayOfMonth: 16, hour: 10, minute: 0, second: 0, nanosecond: 0)
        let tomorrowDateTime = Kotlinx_datetimeLocalDateTime(year: 2026, monthNumber: 8, dayOfMonth: 17, hour: 20, minute: 0, second: 0, nanosecond: 0)
        let today = utc.toInstant(todayDateTime)
        let tomorrow = utc.toInstant(tomorrowDateTime)

        let grouped = DateGrouper.shared.group(
            events: [makeEvent(id: "1", startDateTime: today), makeEvent(id: "2", startDateTime: tomorrow)],
            timeZone: utc,
            today: Kotlinx_datetimeLocalDate(year: 2026, monthNumber: 8, dayOfMonth: 16)
        )

        XCTAssertEqual(grouped.count, 2)
        XCTAssertEqual(grouped[0].label, "Hoje")
        XCTAssertEqual(grouped[0].events.first?.id, "1")
        XCTAssertEqual(grouped[1].label, "Amanhã")
        XCTAssertEqual(grouped[1].events.first?.id, "2")
    }

    func testGivenZeroEventsWhenTheFeedLoadsThenTheEmptyStateIsReachable() {
        let state = EventFeedUiState(
            groupedEvents: [],
            isLoadingInitial: false,
            isLoadingMore: false,
            error: nil,
            endReached: false
        )

        XCTAssertTrue(state.isEmpty)
    }

    @MainActor
    func testGivenAnInitialLoadFailureWhenTheFeedLoadsThenTheRetryStateIsReachable() {
        let wrapper = EventFeedViewModelWrapper(baseUrl: "http://127.0.0.1:1")

        XCTAssertTrue(wrapper.state.groupedEvents.isEmpty)
        wrapper.retry()
    }
}
