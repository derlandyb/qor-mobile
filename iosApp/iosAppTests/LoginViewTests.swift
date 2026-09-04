import XCTest
import ViewInspector
import shared
@testable import iosApp

private final class NeverFanAuthenticator: FanAuthenticating {
    func executeWithPassword(
        email: String,
        password: String,
        completionHandler: @escaping (LoginResult?, Error?) -> Void
    ) {
        // Intentionally never calls back — render assertions below never submit.
    }
}

@MainActor
private func makeView(
    onLoginSuccess: @escaping () -> Void = {},
    onNavigateToVerifyEmail: @escaping (String) -> Void = { _ in },
    onNavigateToSignup: @escaping () -> Void = {},
    onNavigateToPasswordRecovery: @escaping () -> Void = {}
) -> LoginView {
    LoginView(
        onLoginSuccess: onLoginSuccess,
        onNavigateToVerifyEmail: onNavigateToVerifyEmail,
        onNavigateToSignup: onNavigateToSignup,
        onNavigateToPasswordRecovery: onNavigateToPasswordRecovery,
        viewModel: LoginViewModel(authenticateFan: NeverFanAuthenticator())
    )
}

@MainActor
final class LoginViewTests: XCTestCase {
    func test_GIVEN_theLoginScreen_WHEN_itRenders_THEN_theGoogleStubButtonIsDisabled() throws {
        let view = makeView()

        let button = try view.inspect().find(viewWithId: "login_google_stub").button()

        XCTAssertTrue(try button.isDisabled())
    }

    func test_GIVEN_theLoginScreen_WHEN_emptyFieldsAreSubmitted_THEN_validationErrorTextAppears() throws {
        let view = makeView()

        let button = try view.inspect().find(button: String(localized: "cta_entrar"))
        try button.tap()

        let emailErrorText = try view.inspect().find(text: String(localized: "login_error_email_required"))
        XCTAssertNotNil(try emailErrorText.string())
    }

    func test_GIVEN_theLoginScreen_WHEN_itRenders_THEN_theForgotPasswordLinkIsPresent() throws {
        let view = makeView()

        let link = try view.inspect().find(viewWithId: "login_link_password_recovery")

        XCTAssertNotNil(link)
    }

    func test_GIVEN_theForgotPasswordLink_WHEN_tapped_THEN_theNavigationCallbackFires() throws {
        var navigated = false
        let view = makeView(onNavigateToPasswordRecovery: { navigated = true })

        let link = try view.inspect().find(viewWithId: "login_link_password_recovery")
        try link.callOnTapGesture()

        XCTAssertTrue(navigated)
    }

    func test_GIVEN_theSignupLink_WHEN_tapped_THEN_theNavigationCallbackFires() throws {
        var navigated = false
        let view = makeView(onNavigateToSignup: { navigated = true })

        let link = try view.inspect().find(viewWithId: "login_link_signup")
        try link.callOnTapGesture()

        XCTAssertTrue(navigated)
    }
}
