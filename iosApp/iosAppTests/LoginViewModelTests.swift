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
        id: "u1",
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
        let vm = LoginViewModel(authenticateFan: FakeFanAuthenticator(result: LoginResult.InvalidCredentials(message: "n/a")))

        vm.onSubmit()

        XCTAssertEqual(vm.uiState.emailError, .required)
        XCTAssertEqual(vm.uiState.passwordError, .required)
    }

    func test_GIVEN_anInvalidEmailFormat_WHEN_onSubmitIsCalled_THEN_anEmailFormatErrorIsSet() {
        let vm = LoginViewModel(authenticateFan: FakeFanAuthenticator(result: LoginResult.InvalidCredentials(message: "n/a")))
        vm.onEmailChange("not-an-email")
        vm.onPasswordChange("senha123")

        vm.onSubmit()

        XCTAssertEqual(vm.uiState.emailError, .invalidFormat)
        XCTAssertNil(vm.uiState.passwordError)
    }

    func test_GIVEN_aFieldWasInvalid_WHEN_itIsEditedAgain_THEN_itsErrorIsCleared() {
        let vm = LoginViewModel(authenticateFan: FakeFanAuthenticator(result: LoginResult.InvalidCredentials(message: "n/a")))
        vm.onSubmit()
        XCTAssertEqual(vm.uiState.emailError, .required)

        vm.onEmailChange("a")

        XCTAssertNil(vm.uiState.emailError)
    }

    func test_GIVEN_validFields_WHEN_theRepositoryReturnsInvalidCredentials_THEN_theSubmitErrorIsSetAndLoadingEnds() async {
        let vm = LoginViewModel(
            authenticateFan: FakeFanAuthenticator(result: LoginResult.InvalidCredentials(message: "Credenciais inválidas."))
        )
        vm.onEmailChange("ana@example.com")
        vm.onPasswordChange("senha123")

        vm.onSubmit()
        await waitUntil { vm.uiState.isLoading == false }

        XCTAssertEqual(vm.uiState.submitError, .invalidCredentials)
        XCTAssertFalse(vm.uiState.isLoading)
    }

    func test_GIVEN_validFields_WHEN_theRepositoryReturnsUnverifiedAccount_THEN_theNavigationCallbackCarriesTheEmail() async {
        let vm = LoginViewModel(
            authenticateFan: FakeFanAuthenticator(result: LoginResult.UnverifiedAccount(message: "Confirme seu e-mail."))
        )
        var navigatedEmail: String?
        vm.onNavigateToVerifyEmail = { navigatedEmail = $0 }
        vm.onEmailChange("ana@example.com")
        vm.onPasswordChange("senha123")

        vm.onSubmit()
        await waitUntil { vm.uiState.isLoading == false }

        XCTAssertEqual(vm.uiState.submitError, .unverifiedAccount(email: "ana@example.com"))
        XCTAssertEqual(navigatedEmail, "ana@example.com")
    }

    func test_GIVEN_validFields_WHEN_theRepositoryReturnsSuccess_THEN_theLoginSuccessCallbackFires() async {
        let vm = LoginViewModel(
            authenticateFan: FakeFanAuthenticator(result: LoginResult.Success(user: sampleUser(), token: "tok-1"))
        )
        var loggedIn = false
        vm.onLoginSuccess = { loggedIn = true }
        vm.onEmailChange("ana@example.com")
        vm.onPasswordChange("senha123")

        vm.onSubmit()
        await waitUntil { vm.uiState.isLoading == false }

        XCTAssertTrue(loggedIn)
        XCTAssertNil(vm.uiState.submitError)
    }
}

/// Polls a `@MainActor` condition until true or a short timeout elapses — `onSubmit()` fires a
/// detached `Task`, so tests await its completion this way rather than sleeping a fixed duration.
@MainActor
private func waitUntil(
    timeout: TimeInterval = 2,
    _ condition: @escaping () -> Bool
) async {
    let deadline = Date().addingTimeInterval(timeout)
    while !condition(), Date() < deadline {
        try? await Task.sleep(nanoseconds: 5_000_000)
    }
}
