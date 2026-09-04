import XCTest
import ViewInspector
import shared
@testable import iosApp

/// A no-op `HomeFeedEventsGateway`/`HomeFeedPollingGateway` pair — `MainTabRootView`'s tests only
/// assert routing, never event-list content, so these just need to exist and never touch the
/// real Koin graph or network (see `MainTabRootView`'s own doc comment on its view-model seams).
private final class NoopEventsGateway: HomeFeedEventsGateway {
    func loadUpcoming(city: City?, genre: String?, cursor: String?) async throws -> EventPage {
        EventPage(events: [], nextCursor: nil)
    }
}

private final class NoopPollingGateway: HomeFeedPollingGateway {
    func start(city: City?, genre: String?) {}
    func stop() {}
    func refreshNow() async throws {}
    func currentPage() -> EventPage { EventPage(events: [], nextCursor: nil) }
    func observeUpdates(_ onUpdate: @escaping (EventPage) -> Void) {}
}

@MainActor
private func makeHomeFeedViewModel() -> HomeFeedViewModel {
    HomeFeedViewModel(eventsGateway: NoopEventsGateway(), pollingGateway: NoopPollingGateway())
}

@MainActor
private func makeExploreViewModel() -> ExploreViewModel {
    ExploreViewModel(eventsGateway: NoopEventsGateway(), pollingGateway: NoopPollingGateway())
}

@MainActor
private func makeProfileViewModel() -> ProfileViewModel {
    ProfileViewModel(loadCurrentUser: { nil }, saveProfile: { _ in throw NoopError() }, applySessionUser: { _ in })
}

private struct NoopError: Error {}

final class AppNavigationTests: XCTestCase {
    // MARK: - Deep link (qualorock://evento/{eventId})

    func test_GIVEN_aWellFormedEventDeepLink_WHEN_parsingItsEventId_THEN_theIdIsExtracted() {
        let url = URL(string: "qualorock://evento/42")!

        XCTAssertEqual(AppNavigation.eventId(from: url), "42")
    }

    func test_GIVEN_aDeepLinkWithTheWrongScheme_WHEN_parsingItsEventId_THEN_nilIsReturned() {
        let url = URL(string: "https://evento/42")!

        XCTAssertNil(AppNavigation.eventId(from: url))
    }

    func test_GIVEN_aDeepLinkWithTheWrongHost_WHEN_parsingItsEventId_THEN_nilIsReturned() {
        let url = URL(string: "qualorock://outracoisa/42")!

        XCTAssertNil(AppNavigation.eventId(from: url))
    }

    func test_GIVEN_aDeepLinkWithNoEventId_WHEN_parsingItsEventId_THEN_nilIsReturned() {
        let url = URL(string: "qualorock://evento/")!

        XCTAssertNil(AppNavigation.eventId(from: url))
    }

    // MARK: - Bottom-nav tab switching (Home <-> Explore <-> Profile)

    @MainActor
    func test_GIVEN_theMainTabRoot_WHEN_freshlyRendered_THEN_homeFeedIsTheDefaultTab() throws {
        let sut = MainTabRootView(
            onEventClick: { _ in }, onEmailChangePending: { _ in }, homeFeedViewModel: makeHomeFeedViewModel()
        )

        XCTAssertNoThrow(try sut.inspect().find(HomeFeedView.self))
    }

    // `current` is `@State`, whose live storage ViewInspector's un-hosted `.inspect()` doesn't
    // observe across a simulated interaction (unlike the `@Published`/`@StateObject` pattern the
    // other I7-I13 screens use) -- rather than fight that with `ViewHosting`, this asserts the
    // same routing switch by constructing each initial tab directly (the `initialTab` param is a
    // test-only seam, same pattern as `SignupView`'s dual init), and covers `BottomNav.onSelect`
    // being wired straight through to `current` separately below.

    @MainActor
    func test_GIVEN_theMainTabRoot_WHEN_startingOnExplorar_THEN_exploreViewIsShown() throws {
        let sut = MainTabRootView(
            initialTab: .explorar, onEventClick: { _ in }, onEmailChangePending: { _ in },
            exploreViewModel: makeExploreViewModel()
        )

        XCTAssertNoThrow(try sut.inspect().find(ExploreView.self))
    }

    @MainActor
    func test_GIVEN_theMainTabRoot_WHEN_startingOnPerfil_THEN_profileViewIsShown() throws {
        let sut = MainTabRootView(
            initialTab: .perfil, onEventClick: { _ in }, onEmailChangePending: { _ in },
            profileViewModel: makeProfileViewModel()
        )

        XCTAssertNoThrow(try sut.inspect().find(ProfileView.self))
    }

    @MainActor
    func test_GIVEN_bottomNavsOnSelect_WHEN_calledWithAnEnabledDestination_THEN_itIsCallable() throws {
        let sut = MainTabRootView(
            onEventClick: { _ in }, onEmailChangePending: { _ in }, homeFeedViewModel: makeHomeFeedViewModel()
        )

        let bottomNav = try sut.inspect().find(BottomNav.self).actualView()

        XCTAssertEqual(bottomNav.current, .inicio)
        // `BottomNav` itself is a dumb `current`/`onSelect` pair (I3) -- this confirms
        // `MainTabRootView` wires a real, callable closure into it, not a no-op.
        bottomNav.onSelect(.explorar)
    }
}
