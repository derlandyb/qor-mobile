import ViewInspector
import XCTest
@testable import iosApp
import shared

/// I10 — render/state assertions for [PasswordRecoveryView], complementing
/// `PasswordRecoveryViewModelTests`'s pure logic coverage. Drives the view model directly
/// (never through the UI) to reach each of the 3 wizard steps plus the success screen, then
/// asserts the corresponding step's `accessibilityIdentifier` container is the one rendered —
/// robust to layout/copy changes, unlike positional `VStack` indexing.
@MainActor
final class PasswordRecoveryViewTests: XCTestCase {
    private func makeView(
        verifyResult: VerifyResetCodeResult = VerifyResetCodeResult.Success(token: "tok"),
        confirmResult: ConfirmResetResult = ConfirmResetResult.Success()
    ) -> (PasswordRecoveryView, PasswordRecoveryViewModel) {
        let repository = FakeUserRepository(verifyResult: verifyResult, confirmResult: confirmResult)
        let viewModel = PasswordRecoveryViewModel(resetPassword: ResetPassword(userRepository: repository))
        let view = PasswordRecoveryView(viewModel: viewModel, onResetSuccess: {}, onNavigateToLogin: {})
        return (view, viewModel)
    }

    func test_GIVEN_theViewJustAppeared_THEN_step1RequestEmailContentIsShown() throws {
        let (view, _) = makeView()

        XCTAssertNoThrow(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_step_request_email"))
        XCTAssertThrowsError(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_step_verify_code"))
        XCTAssertThrowsError(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_step_new_password"))
        XCTAssertThrowsError(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_step_success"))
    }

    func test_GIVEN_theViewModelAdvancedToStep2_THEN_verifyCodeContentIsShownAndStep1IsNot() async throws {
        let (view, viewModel) = makeView()
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()

        XCTAssertNoThrow(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_step_verify_code"))
        XCTAssertThrowsError(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_step_request_email"))
    }

    func test_GIVEN_theViewModelAdvancedToStep3_THEN_newPasswordContentIsShown() async throws {
        let (view, viewModel) = makeView(verifyResult: .Success(token: "real-token"))
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123456")
        await viewModel.onSubmitCode()

        XCTAssertNoThrow(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_step_new_password"))
        XCTAssertThrowsError(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_step_verify_code"))
    }

    func test_GIVEN_theResetSucceeded_THEN_theSuccessScreenIsShownAndTheLoginLinkIsHidden() async throws {
        let (view, viewModel) = makeView(verifyResult: .Success(token: "real-token"), confirmResult: .Success())
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123456")
        await viewModel.onSubmitCode()
        viewModel.onNewPasswordChange("supersenha123")
        await viewModel.onSubmitNewPassword()

        XCTAssertNoThrow(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_step_success"))
        XCTAssertThrowsError(try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_login_link"))
    }

    func test_GIVEN_step2VerifyCodeFails_THEN_theServerMessageIsRenderedInline() async throws {
        let (view, viewModel) = makeView(verifyResult: .Failure(message: "Código inválido ou expirado."))
        viewModel.onEmailChange("ana@example.com")
        await viewModel.onSubmitEmail()
        viewModel.onCodeChange("123456")
        await viewModel.onSubmitCode()

        let errorText = try view.inspect().find(text: "Código inválido ou expirado.")
        XCTAssertNotNil(errorText)
    }

    func test_GIVEN_step1_WHEN_theLoginLinkIsTapped_THEN_onNavigateToLoginFires() throws {
        var navigated = false
        let repository = FakeUserRepository()
        let viewModel = PasswordRecoveryViewModel(resetPassword: ResetPassword(userRepository: repository))
        let view = PasswordRecoveryView(
            viewModel: viewModel,
            onResetSuccess: {},
            onNavigateToLogin: { navigated = true }
        )

        try view.inspect().find(viewWithAccessibilityIdentifier: "password_recovery_login_link").callOnTapGesture()

        XCTAssertTrue(navigated)
    }
}
