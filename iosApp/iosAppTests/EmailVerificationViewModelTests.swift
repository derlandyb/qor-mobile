import XCTest
import shared
@testable import iosApp

/// Minimal fake `UserRepository`, same shape as Android's `FakeVerifyEmailUserRepository` in
/// `EmailVerificationViewModelTest.kt` — every method this test doesn't exercise traps if
/// called, keeping the fake independent of any other use case's contract.
private final class FakeVerifyEmailUserRepository: UserRepository {
    private let verifyResult: VerifyEmailResult
    var lastVerifyEmail: String?
    var lastVerifyCode: String?
    var resendCallCount = 0
    var lastResendEmail: String?

    init(verifyResult: VerifyEmailResult) {
        self.verifyResult = verifyResult
    }

    func verifyEmailCode(email: String, code: String) async throws -> VerifyEmailResult {
        lastVerifyEmail = email
        lastVerifyCode = code
        return verifyResult
    }

    func resendVerification(email: String) async throws {
        resendCallCount += 1
        lastResendEmail = email
    }

    func register(
        email: String, password: String, birthdate: String, name: String, consentAccepted: Bool
    ) async throws -> RegisterResult {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func login(email: String, password: String) async throws -> LoginResult {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func loginWithGoogle(googleIdToken: String) async throws -> LoginResult {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func logout() async throws {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func requestPasswordReset(email: String) async throws {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func verifyResetCode(email: String, code: String) async throws -> VerifyResetCodeResult {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func confirmPasswordReset(email: String, token: String, newPassword: String) async throws -> ConfirmResetResult {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func getProfile() async throws -> User {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func updateProfile(fields: ProfileUpdateFields) async throws -> User {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func accessData() async throws -> DataRightResult {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func exportData() async throws -> DataRightResult {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func deleteAccount() async throws -> DataRightResult {
        fatalError("not used by EmailVerificationViewModelTests")
    }

    func revokeConsent(consentType: ConsentType) async throws -> DataRightResult {
        fatalError("not used by EmailVerificationViewModelTests")
    }
}

private func sampleUser() -> User {
    User(
        id: "u1", name: "Ana", email: "ana@example.com", emailVerifiedAt: "2026-01-01T00:00:00Z",
        phone: nil, profilePictureUrl: nil, birthdate: "1990-01-01"
    )
}

@MainActor
final class EmailVerificationViewModelTests: XCTestCase {
    private func makeViewModel(
        verifyResult: VerifyEmailResult = VerifyEmailResult.Failure(message: "n/a"),
        context: EmailVerificationContext = .signup
    ) -> (viewModel: EmailVerificationViewModel, repository: FakeVerifyEmailUserRepository, signupCalls: Box, emailChangeCalls: Box) {
        let repository = FakeVerifyEmailUserRepository(verifyResult: verifyResult)
        let verifyEmail = VerifyEmail(userRepository: repository)
        let signupCalls = Box()
        let emailChangeCalls = Box()
        let viewModel = EmailVerificationViewModel(
            context: context,
            verifyEmail: verifyEmail,
            onVerifiedForSignup: { signupCalls.count += 1 },
            onVerifiedForEmailChange: { emailChangeCalls.count += 1 }
        )
        return (viewModel, repository, signupCalls, emailChangeCalls)
    }

    /// Plain reference-type counter — closures need a shared mutable box to record calls.
    final class Box {
        var count = 0
    }

    func test_GIVEN_itIsConstructed_THEN_theCooldownStartsAtTheConfiguredResendInterval() {
        let (viewModel, _, _, _) = makeViewModel()

        XCTAssertEqual(
            viewModel.uiState.cooldown.remainingSeconds, Int(QorConfig.shared.EmailVerificationResendCooldownSeconds)
        )
        XCTAssertFalse(viewModel.uiState.cooldown.canResend)
    }

    func test_GIVEN_anEmptyCode_WHEN_onSubmitIsCalled_THEN_aRequiredFieldErrorIsSet() async {
        let (viewModel, _, _, _) = makeViewModel()

        await viewModel.onSubmit(email: "ana@example.com")

        XCTAssertEqual(viewModel.uiState.codeError, .required)
    }

    func test_GIVEN_aCodeShorterThanSixDigits_WHEN_onSubmitIsCalled_THEN_anInvalidLengthErrorIsSet() async {
        let (viewModel, _, _, _) = makeViewModel()
        viewModel.onCodeChange("123")

        await viewModel.onSubmit(email: "ana@example.com")

        XCTAssertEqual(viewModel.uiState.codeError, .invalidLength)
    }

    func test_GIVEN_aCodeWasInvalid_WHEN_itIsEditedAgain_THEN_itsErrorIsCleared() async {
        let (viewModel, _, _, _) = makeViewModel()
        await viewModel.onSubmit(email: "ana@example.com")
        XCTAssertEqual(viewModel.uiState.codeError, .required)

        viewModel.onCodeChange("1")

        XCTAssertNil(viewModel.uiState.codeError)
    }

    func test_GIVEN_aValidCodeAndSignupContext_WHEN_theUseCaseReturnsSuccess_THEN_onVerifiedForSignupFires() async {
        let (viewModel, repository, signupCalls, emailChangeCalls) = makeViewModel(
            verifyResult: VerifyEmailResult.Success(user: sampleUser()), context: .signup
        )
        viewModel.onCodeChange("123456")

        await viewModel.onSubmit(email: "ana@example.com")

        XCTAssertEqual(signupCalls.count, 1)
        XCTAssertEqual(emailChangeCalls.count, 0)
        XCTAssertEqual(repository.lastVerifyEmail, "ana@example.com")
        XCTAssertEqual(repository.lastVerifyCode, "123456")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertNil(viewModel.uiState.submitError)
    }

    func test_GIVEN_aValidCodeAndEmailChangeContext_WHEN_theUseCaseReturnsSuccess_THEN_onVerifiedForEmailChangeFires() async {
        let (viewModel, _, signupCalls, emailChangeCalls) = makeViewModel(
            verifyResult: VerifyEmailResult.Success(user: sampleUser()), context: .emailChange
        )
        viewModel.onCodeChange("123456")

        await viewModel.onSubmit(email: "ana@example.com")

        XCTAssertEqual(emailChangeCalls.count, 1)
        XCTAssertEqual(signupCalls.count, 0)
    }

    func test_GIVEN_aValidCode_WHEN_theUseCaseReturnsFailure_THEN_theServerMessageIsShownAsAnInlineError() async {
        let (viewModel, _, _, _) = makeViewModel(
            verifyResult: VerifyEmailResult.Failure(message: "Código inválido ou expirado.")
        )
        viewModel.onCodeChange("123456")

        await viewModel.onSubmit(email: "ana@example.com")

        XCTAssertEqual(viewModel.uiState.submitError, "Código inválido ou expirado.")
        XCTAssertFalse(viewModel.uiState.isLoading)
    }

    func test_GIVEN_theCooldownHasNotElapsed_WHEN_onResendIsCalled_THEN_itIsIgnored() async {
        let (viewModel, repository, _, _) = makeViewModel()

        await viewModel.onResend(email: "ana@example.com")

        XCTAssertEqual(repository.resendCallCount, 0)
    }

    func test_GIVEN_theCooldownHasElapsed_WHEN_onResendIsCalled_THEN_resendFiresAndTheCooldownRestarts() async {
        let (viewModel, repository, _, _) = makeViewModel()
        viewModel.forceCooldownElapsedForTesting()
        XCTAssertTrue(viewModel.uiState.cooldown.canResend)

        await viewModel.onResend(email: "ana@example.com")

        XCTAssertEqual(repository.resendCallCount, 1)
        XCTAssertEqual(repository.lastResendEmail, "ana@example.com")
        XCTAssertTrue(viewModel.uiState.resendConfirmation)
        XCTAssertEqual(
            viewModel.uiState.cooldown.remainingSeconds, Int(QorConfig.shared.EmailVerificationResendCooldownSeconds)
        )
        XCTAssertFalse(viewModel.uiState.cooldown.canResend)
    }
}

final class VerificationCooldownTests: XCTestCase {
    func test_GIVEN_theDefaultInterval_WHEN_started_THEN_itMatchesTheSharedConfigConstant() {
        let cooldown = VerificationCooldown.started()

        XCTAssertEqual(cooldown.remainingSeconds, Int(QorConfig.shared.EmailVerificationResendCooldownSeconds))
        XCTAssertFalse(cooldown.canResend)
    }

    func test_GIVEN_aPositiveRemainingCount_WHEN_ticked_THEN_itDecrementsByOne() {
        let cooldown = VerificationCooldown(remainingSeconds: 5)

        XCTAssertEqual(cooldown.ticked().remainingSeconds, 4)
    }

    func test_GIVEN_zeroRemainingSeconds_WHEN_ticked_THEN_itIsFlooredAtZeroAndCanResend() {
        let cooldown = VerificationCooldown(remainingSeconds: 0)

        XCTAssertEqual(cooldown.ticked().remainingSeconds, 0)
        XCTAssertTrue(cooldown.canResend)
    }
}
