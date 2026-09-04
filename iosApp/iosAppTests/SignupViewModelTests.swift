import XCTest
import shared
@testable import iosApp

private struct FakeSignupRegistering: SignupRegistering {
    var result: Result<RegisterResult, Error> = .success(
        RegisterResult.Success(user: User(
            id: 1, name: "Ana", email: "ana@example.com", emailVerifiedAt: nil,
            phone: nil, profilePictureUrl: nil, birthdate: "2000-01-01"
        ))
    )
    var receivedArgs: ((String, String, String, String, Bool) -> Void)?

    func execute(
        email: String, password: String, birthdate: String, name: String, consentAccepted: Bool
    ) async throws -> RegisterResult {
        receivedArgs?(email, password, birthdate, name, consentAccepted)
        return try result.get()
    }
}

private struct FakeError: Error, LocalizedError {
    var errorDescription: String? { "network error" }
}

@MainActor
final class SignupViewModelTests: XCTestCase {
    private func makeSut(
        registering: SignupRegistering = FakeSignupRegistering(),
        onSuccess: @escaping (String) -> Void = { _ in }
    ) -> SignupViewModel {
        SignupViewModel(registerFan: registering, onSignupSuccess: onSuccess)
    }

    func test_GIVEN_emptyForm_WHEN_submitting_THEN_everyRequiredFieldShowsAnError() async {
        let sut = makeSut()

        await sut.submit()

        XCTAssertEqual(sut.uiState.nameError, .required)
        XCTAssertEqual(sut.uiState.emailError, .required)
        XCTAssertEqual(sut.uiState.passwordError, .required)
        XCTAssertEqual(sut.uiState.birthdateError, .required)
        XCTAssertEqual(sut.uiState.consentError, .required)
        XCTAssertFalse(sut.uiState.isLoading)
    }

    func test_GIVEN_invalidEmail_WHEN_submitting_THEN_emailErrorIsInvalidFormat() async {
        let sut = makeSut()
        sut.onNameChange("Ana")
        sut.onEmailChange("not-an-email")
        sut.onPasswordChange("secret123")
        sut.onConfirmPasswordChange("secret123")
        sut.onBirthdateChange("2000-01-01")
        sut.onConsentChange(true)

        await sut.submit()

        XCTAssertEqual(sut.uiState.emailError, .invalidFormat)
    }

    func test_GIVEN_mismatchedPasswords_WHEN_submitting_THEN_confirmPasswordErrorIsMismatch() async {
        let sut = makeSut()
        sut.onNameChange("Ana")
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("secret123")
        sut.onConfirmPasswordChange("different")
        sut.onBirthdateChange("2000-01-01")
        sut.onConsentChange(true)

        await sut.submit()

        XCTAssertEqual(sut.uiState.confirmPasswordError, .mismatch)
    }

    func test_GIVEN_consentNotAccepted_WHEN_submitting_THEN_consentErrorIsRequired() async {
        let sut = makeSut()
        sut.onNameChange("Ana")
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("secret123")
        sut.onConfirmPasswordChange("secret123")
        sut.onBirthdateChange("2000-01-01")

        await sut.submit()

        XCTAssertEqual(sut.uiState.consentError, .required)
    }

    func test_GIVEN_validForm_WHEN_submitting_THEN_registerFanIsCalledWithFormValues() async {
        var captured: (String, String, String, String, Bool)?
        let registering = FakeSignupRegistering(receivedArgs: { email, password, birthdate, name, consent in
            captured = (email, password, birthdate, name, consent)
        })
        let sut = makeSut(registering: registering)
        sut.onNameChange("Ana")
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("secret123")
        sut.onConfirmPasswordChange("secret123")
        sut.onBirthdateChange("2000-01-01")
        sut.onConsentChange(true)

        await sut.submit()

        XCTAssertEqual(captured?.0, "ana@example.com")
        XCTAssertEqual(captured?.1, "secret123")
        XCTAssertEqual(captured?.2, "2000-01-01")
        XCTAssertEqual(captured?.3, "Ana")
        XCTAssertEqual(captured?.4, true)
    }

    func test_GIVEN_validForm_WHEN_registerSucceeds_THEN_onSignupSuccessIsCalledWithEmail() async {
        var successEmail: String?
        let sut = makeSut(onSuccess: { successEmail = $0 })
        sut.onNameChange("Ana")
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("secret123")
        sut.onConfirmPasswordChange("secret123")
        sut.onBirthdateChange("2000-01-01")
        sut.onConsentChange(true)

        await sut.submit()

        XCTAssertEqual(successEmail, "ana@example.com")
        XCTAssertFalse(sut.uiState.isLoading)
    }

    func test_GIVEN_serverReturnsFieldErrors_WHEN_submitting_THEN_matchingFieldsShowServerErrors() async {
        let failure = RegisterResult.Failure(
            message: "Erro de validação",
            fieldErrors: ["email": ["Este e-mail já está em uso."]]
        )
        let registering = FakeSignupRegistering(result: .success(failure))
        let sut = makeSut(registering: registering)
        sut.onNameChange("Ana")
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("secret123")
        sut.onConfirmPasswordChange("secret123")
        sut.onBirthdateChange("2000-01-01")
        sut.onConsentChange(true)

        await sut.submit()

        XCTAssertEqual(sut.uiState.emailServerError, "Este e-mail já está em uso.")
        XCTAssertNil(sut.uiState.submitError)
        XCTAssertFalse(sut.uiState.isLoading)
    }

    func test_GIVEN_serverReturnsUnknownFailure_WHEN_submitting_THEN_submitErrorShowsGenericMessage() async {
        let failure = RegisterResult.Failure(message: "Algo deu errado.", fieldErrors: [:])
        let registering = FakeSignupRegistering(result: .success(failure))
        let sut = makeSut(registering: registering)
        sut.onNameChange("Ana")
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("secret123")
        sut.onConfirmPasswordChange("secret123")
        sut.onBirthdateChange("2000-01-01")
        sut.onConsentChange(true)

        await sut.submit()

        XCTAssertEqual(sut.uiState.submitError, "Algo deu errado.")
    }

    func test_GIVEN_networkThrows_WHEN_submitting_THEN_submitErrorIsSetAndLoadingStops() async {
        let registering = FakeSignupRegistering(result: .failure(FakeError()))
        let sut = makeSut(registering: registering)
        sut.onNameChange("Ana")
        sut.onEmailChange("ana@example.com")
        sut.onPasswordChange("secret123")
        sut.onConfirmPasswordChange("secret123")
        sut.onBirthdateChange("2000-01-01")
        sut.onConsentChange(true)

        await sut.submit()

        XCTAssertEqual(sut.uiState.submitError, "network error")
        XCTAssertFalse(sut.uiState.isLoading)
    }

    func test_GIVEN_fieldHasError_WHEN_userEditsThatField_THEN_itsErrorClears() async {
        let sut = makeSut()
        await sut.submit()
        XCTAssertNotNil(sut.uiState.nameError)

        sut.onNameChange("A")

        XCTAssertNil(sut.uiState.nameError)
    }
}

extension NameFieldError: Equatable {}
extension SignupEmailFieldError: Equatable {}
extension SignupPasswordFieldError: Equatable {}
extension ConfirmPasswordFieldError: Equatable {}
extension BirthdateFieldError: Equatable {}
extension ConsentFieldError: Equatable {}
