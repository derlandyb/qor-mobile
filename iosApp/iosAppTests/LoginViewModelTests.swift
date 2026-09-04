import XCTest
import shared
@testable import iosApp

/// Fakes `AuthenticateFan`'s completion-handler surface via ``FanAuthenticating`` so
/// `LoginViewModel` tests don't need a real Koin graph — mirrors Android's `FakeUserRepository`
/// intent (exercise the view model against a controlled result) without the KMP protocol
/// boilerplate a full `UserRepository` fake would require.
private final class FakeFanAuthenticator: FanAuthenticating {
    private let result: LoginResult
    private(set) var lastEmail: String?
    private(set) var lastPassword: String?

    init(result: LoginResult) {
        self.result = result
    }

    func executeWithPassword(
        email: String,
        password: String,
        completionHandler: @escaping (LoginResult?, Error?) -> Void
    ) {
        lastEmail = email
        lastPassword = password
        completionHandler(result, nil)
    }
}

private func sampleUser() -> User {
    User(
        id: 1,
        name: "Ana",
        email: "ana@example.com",
        emailVerifiedAt: "2026-01-01T00:00:00Z",
        phone: nil,
        profilePictureUrl: nil,
        birthdate: "1990-01-01"
    )
}

@MainActor
final class LoginViewModelTests: XCTestCase {
    func test_GIVEN_bothFieldsAreEmpty_WHEN_onSubmitIsCalled_THEN_requiredFieldErrorsAreSet() {
        let sut = LoginViewModel(
            authenticateFan: FakeFanAuthenticator(result: LoginResult.InvalidCredentials(message: "n/a"))
        )

        sut.onSubmit()

        XCTAssertEqual(sut.uiState.emailError, .required)
        XCTAssertEqual(sut.uiState.passwordError, .required)
    }

    func test_GIVEN_anInvalidEmailFormat_WHEN_onSubmitIsCalled_THEN_anEmailFormatErrorIsSet() {
        let sut = LoginViewModel(
            authenticateFan: FakeFanAuthenticator(result: LoginResult.InvalidCredentials(message: "n/a"))
        )
        sut.onEmailChange("not-an-email")
        sut.onPasswordChange("senha123")

        sut.onSubmit()

        XCTAssertEqual(sut.uiState.emailError, .invalidFormat)
        XCTAssertNil(sut.uiState.passwordError)
    }

    func test_GIVEN_aFieldWasInvalid_WHEN_itIsEditedAgain_THEN_itsErrorIsCleared() {
        let sut = LoginViewModel(
            authenticateFan: FakeFanAuthenticator(result: LoginResult.InvalidCredentials(message: "n/a"))
        )
        sut.onSubmit()
        XCTAssertEqual(sut.uiState.emailError, .required)

        sut.onEmailChange("a")

        XCTAssertNil(sut.uiState.emailError)
    }

    func test_GIVEN_validFields_WHEN_repoReturnsInvalidCredentials_THEN_submitErrorIsSetAndLoadingEnds() async {
        let invalidCredentials = LoginResult.InvalidCredentials(message: "Credenciais inválidas.")
        let sut = LoginViewModel(authenticateFan: FakeFanAuthenticator(result: invalidCredentials))
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("senha123")

        sut.onSubmit()
        await waitUntil { sut.uiState.isLoading == false }

        XCTAssertEqual(sut.uiState.submitError, .invalidCredentials)
        XCTAssertFalse(sut.uiState.isLoading)
    }

    func test_GIVEN_validFields_WHEN_repoReturnsUnverifiedAccount_THEN_navigationCallbackCarriesTheEmail() async {
        let unverified = LoginResult.UnverifiedAccount(message: "Confirme seu e-mail.")
        let sut = LoginViewModel(
            authenticateFan: FakeFanAuthenticator(result: unverified)
        )
        var navigatedEmail: String?
        sut.onNavigateToVerifyEmail = { navigatedEmail = $0 }
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("senha123")

        sut.onSubmit()
        await waitUntil { sut.uiState.isLoading == false }

        XCTAssertEqual(sut.uiState.submitError, .unverifiedAccount(email: "ana@example.com"))
        XCTAssertEqual(navigatedEmail, "ana@example.com")
    }

    func test_GIVEN_validFields_WHEN_theRepositoryReturnsSuccess_THEN_theLoginSuccessCallbackFires() async {
        let sut = LoginViewModel(
            authenticateFan: FakeFanAuthenticator(result: LoginResult.Success(user: sampleUser(), token: "tok-1"))
        )
        var loggedIn = false
        sut.onLoginSuccess = { loggedIn = true }
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("senha123")

        sut.onSubmit()
        await waitUntil { sut.uiState.isLoading == false }

        XCTAssertTrue(loggedIn)
        XCTAssertNil(sut.uiState.submitError)
    }
}

/// Polls a `@MainActor` condition until true or a short timeout elapses — `onSubmit()` fires a
/// detached `Task`, so tests await its completion this way rather than sleeping a fixed duration.
@MainActor
private func waitUntil(
    timeout: TimeInterval = 5,
    _ condition: @escaping () -> Bool
) async {
    let deadline = Date().addingTimeInterval(timeout)
    while !condition(), Date() < deadline {
        try? await Task.sleep(nanoseconds: 5_000_000)
    }
}
