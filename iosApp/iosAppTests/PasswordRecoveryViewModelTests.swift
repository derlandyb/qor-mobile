import XCTest
import shared
@testable import iosApp

/// Task-scoped in-file `UserRepository` fake, mirroring Android's
/// `FakePasswordRecoveryUserRepository` — keeps this test independent of Koin/the real Ktor
/// implementation. Every method [PasswordRecoveryViewModel] doesn't exercise fails loudly if hit.
final class FakeUserRepository: UserRepository {
    private let verifyResult: (String, String) -> VerifyResetCodeResult
    private let confirmResult: () -> ConfirmResetResult

    private(set) var requestResetCallCount = 0
    private(set) var lastRequestResetEmail: String?
    private(set) var lastVerifyEmail: String?
    private(set) var lastVerifyCode: String?
    private(set) var lastConfirmEmail: String?
    private(set) var lastConfirmToken: String?
    private(set) var lastConfirmPassword: String?

    init(
        verifyResult: VerifyResetCodeResult = VerifyResetCodeResult.Failure(message: "n/a"),
        confirmResult: @escaping @autoclosure () -> ConfirmResetResult = ConfirmResetResult.Failure(message: "n/a")
    ) {
        self.verifyResult = { _, _ in verifyResult }
        self.confirmResult = confirmResult
    }

    /// Lets a test drive `verifyResetCode`'s result off the code it was actually called with —
    /// e.g. to simulate "wrong code fails, right code succeeds" without a second fake type,
    /// mirroring Android's inline `UserRepository by ... { override fun verifyResetCode ... }`.
    init(
        verifyResult: @escaping (String, String) -> VerifyResetCodeResult,
        confirmResult: @escaping @autoclosure () -> ConfirmResetResult = ConfirmResetResult.Failure(message: "n/a")
    ) {
        self.verifyResult = verifyResult
        self.confirmResult = confirmResult
    }

    func requestPasswordReset(email: String) async throws {
        requestResetCallCount += 1
        lastRequestResetEmail = email
    }

    func verifyResetCode(email: String, code: String) async throws -> VerifyResetCodeResult {
        lastVerifyEmail = email
        lastVerifyCode = code
        return verifyResult(email, code)
    }

    func confirmPasswordReset(email: String, token: String, newPassword: String) async throws -> ConfirmResetResult {
        lastConfirmEmail = email
        lastConfirmToken = token
        lastConfirmPassword = newPassword
        return confirmResult()
    }

    func register(
        email: String, password: String, birthdate: String, name: String, consentAccepted: Bool
    ) async throws -> RegisterResult {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func login(email: String, password: String) async throws -> LoginResult {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func loginWithGoogle(googleIdToken: String) async throws -> LoginResult {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func logout() async throws {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func resendVerification(email: String) async throws {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func verifyEmailCode(email: String, code: String) async throws -> VerifyEmailResult {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func getProfile() async throws -> User {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func updateProfile(fields: ProfileUpdateFields) async throws -> User {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func accessData() async throws -> DataRightResult {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func exportData() async throws -> DataRightResult {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func deleteAccount() async throws -> DataRightResult {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }

    func revokeConsent(consentType: ConsentType) async throws -> DataRightResult {
        fatalError("not used by PasswordRecoveryViewModelTests")
    }
}

@MainActor
final class PasswordRecoveryViewModelTests: XCTestCase {
    private func makeViewModel(
        verifyResult: VerifyResetCodeResult = VerifyResetCodeResult.Failure(message: "n/a"),
        confirmResult: ConfirmResetResult = ConfirmResetResult.Failure(message: "n/a")
    ) -> (PasswordRecoveryViewModel, FakeUserRepository) {
        let repository = FakeUserRepository(verifyResult: verifyResult, confirmResult: confirmResult)
        let viewModel = PasswordRecoveryViewModel(resetPassword: ResetPassword(userRepository: repository))
        return (viewModel, repository)
    }

    func test_GIVEN_itIsConstructed_THEN_step1RequestEmailIsShown() {
        let (viewModel, _) = makeViewModel()

        XCTAssertEqual(viewModel.uiState.step, .requestEmail)
    }

    func test_GIVEN_anEmptyEmail_WHEN_onSubmitEmailIsCalled_THEN_aRequiredFieldErrorIsSet() async {
        let (viewModel, _) = makeViewModel()

        await viewModel.onSubmitEmail()

        XCTAssertEqual(viewModel.uiState.emailError, .required)
        XCTAssertEqual(viewModel.uiState.step, .requestEmail)
    }

    func test_GIVEN_aMalformedEmail_WHEN_onSubmitEmailIsCalled_THEN_anInvalidFormatErrorIsSet() async {
        let (viewModel, _) = makeViewModel()
        viewModel.onEmailChange("not-an-email")

        await viewModel.onSubmitEmail()

        XCTAssertEqual(viewModel.uiState.emailError, .invalidFormat)
    }

    func test_GIVEN_aValidEmail_WHEN_onSubmitEmailIsCalled_THEN_step2AdvancesRegardlessOfBackendResponse() async {
        let (viewModel, repository) = makeViewModel()
        viewModel.onEmailChange("ana@example.com")

        await viewModel.onSubmitEmail()

        XCTAssertEqual(repository.requestResetCallCount, 1)
        XCTAssertEqual(repository.lastRequestResetEmail, "ana@example.com")
        XCTAssertEqual(viewModel.uiState.step, .verifyCode(email: "ana@example.com"))
        XCTAssertFalse(viewModel.uiState.isLoading)
    }

    func test_GIVEN_step2_WHEN_onSubmitCodeIsCalledWithAnEmptyCode_THEN_aRequiredCodeErrorIsSet() async {
        let (viewModel, _) = makeViewModel()
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()

        await viewModel.onSubmitCode()

        XCTAssertEqual(viewModel.uiState.codeError, .required)
    }

    func test_GIVEN_step2_WHEN_onSubmitCodeIsCalledWithAShortCode_THEN_anInvalidLengthErrorIsSet() async {
        let (viewModel, _) = makeViewModel()
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123")

        await viewModel.onSubmitCode()

        XCTAssertEqual(viewModel.uiState.codeError, .invalidLength)
    }

    func test_GIVEN_step2_WHEN_verifyResetCodeReturnsSuccess_THEN_step3AdvancesWithTheReturnedToken() async {
        let (viewModel, repository) = makeViewModel(
            verifyResult: VerifyResetCodeResult.Success(token: "real-token-123")
        )
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123456")

        await viewModel.onSubmitCode()

        XCTAssertEqual(repository.lastVerifyEmail, "ana@example.com")
        XCTAssertEqual(repository.lastVerifyCode, "123456")
        XCTAssertEqual(viewModel.uiState.step, .newPassword(email: "ana@example.com", token: "real-token-123"))
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertNil(viewModel.uiState.submitError)
    }

    func test_GIVEN_step2_WHEN_verifyResetCodeReturnsFailure_THEN_theErrorIsShownInlineAndTheFanStaysOnStep2() async {
        let (viewModel, _) = makeViewModel(
            verifyResult: VerifyResetCodeResult.Failure(message: "Código inválido ou expirado.")
        )
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123456")

        await viewModel.onSubmitCode()

        XCTAssertEqual(viewModel.uiState.submitError, "Código inválido ou expirado.")
        XCTAssertEqual(viewModel.uiState.step, .verifyCode(email: "ana@example.com"))
        XCTAssertFalse(viewModel.uiState.isLoading)
    }

    func test_GIVEN_step2FailedOnce_WHEN_theFanRetriesWithAValidCode_THEN_step3Advances() async {
        let repository = FakeUserRepository(verifyResult: { _, code in
            code == "123456"
                ? VerifyResetCodeResult.Success(token: "real-token-123")
                : VerifyResetCodeResult.Failure(message: "Código inválido ou expirado.")
        })
        let viewModel = PasswordRecoveryViewModel(resetPassword: ResetPassword(userRepository: repository))
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("000000")
        await viewModel.onSubmitCode()
        XCTAssertEqual(viewModel.uiState.step, .verifyCode(email: "ana@example.com"))

        viewModel.onCodeChange("123456")
        await viewModel.onSubmitCode()

        XCTAssertEqual(viewModel.uiState.step, .newPassword(email: "ana@example.com", token: "real-token-123"))
    }

    func test_GIVEN_step3_WHEN_onSubmitNewPasswordIsCalledWithAnEmptyPassword_THEN_aRequiredPasswordErrorIsSet() async {
        let (viewModel, _) = makeViewModel(verifyResult: VerifyResetCodeResult.Success(token: "tok"))
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123456")
        await viewModel.onSubmitCode()

        await viewModel.onSubmitNewPassword()

        XCTAssertEqual(viewModel.uiState.newPasswordError, .required)
    }

    func test_GIVEN_step3_WHEN_onSubmitNewPasswordIsCalledWithATooShortPassword_THEN_aTooShortErrorIsSet() async {
        let (viewModel, _) = makeViewModel(verifyResult: VerifyResetCodeResult.Success(token: "tok"))
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123456")
        await viewModel.onSubmitCode()
        viewModel.onNewPasswordChange("short1")

        await viewModel.onSubmitNewPassword()

        XCTAssertEqual(viewModel.uiState.newPasswordError, .tooShort)
    }

    func test_GIVEN_step3_WHEN_confirmResetReturnsSuccess_THEN_isSuccessFlipsUsingTheStep2Token() async {
        let (viewModel, repository) = makeViewModel(
            verifyResult: VerifyResetCodeResult.Success(token: "real-token-123"),
            confirmResult: ConfirmResetResult.Success()
        )
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123456")
        await viewModel.onSubmitCode()
        viewModel.onNewPasswordChange("supersenha123")

        await viewModel.onSubmitNewPassword()

        XCTAssertTrue(viewModel.uiState.isSuccess)
        XCTAssertEqual(repository.lastConfirmEmail, "ana@example.com")
        XCTAssertEqual(repository.lastConfirmToken, "real-token-123")
        XCTAssertEqual(repository.lastConfirmPassword, "supersenha123")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertNil(viewModel.uiState.submitError)
    }

    func test_GIVEN_step3_WHEN_confirmResetReturnsFailure_THEN_theServerMessageIsShownAsAnInlineError() async {
        let (viewModel, _) = makeViewModel(
            verifyResult: VerifyResetCodeResult.Success(token: "real-token-123"),
            confirmResult: ConfirmResetResult.Failure(message: "Link expirado ou já utilizado.")
        )
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123456")
        await viewModel.onSubmitCode()
        viewModel.onNewPasswordChange("supersenha123")

        await viewModel.onSubmitNewPassword()

        XCTAssertEqual(viewModel.uiState.submitError, "Link expirado ou já utilizado.")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(viewModel.uiState.isSuccess)
    }
}
