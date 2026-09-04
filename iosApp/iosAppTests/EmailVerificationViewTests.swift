import XCTest
import ViewInspector
@testable import iosApp

/// I9 — render assertions for `EmailVerificationView`'s static structure, mirroring the
/// ViewInspector-based render-test pattern this track introduces (see `EmailVerificationViewModelTests`
/// for the logic-level coverage of state transitions this view drives from).
@MainActor
final class EmailVerificationViewTests: XCTestCase {
    private func makeView() -> EmailVerificationView {
        EmailVerificationView(
            email: "ana@example.com",
            context: .signup,
            onVerifiedForSignup: {},
            onVerifiedForEmailChange: {}
        )
    }

    func test_GIVEN_theInitialState_WHEN_rendered_THEN_theOtpFieldAndSubmitButtonArePresent() throws {
        let view = makeView()

        XCTAssertNoThrow(try view.inspect().find(ViewType.TextField.self))
        XCTAssertNoThrow(try view.inspect().find(text: String(localized: "cta_verificar_codigo")))
    }

    func test_GIVEN_theInitialState_WHEN_rendered_THEN_theResendCountdownIsShownNotTheResendLink() throws {
        let view = makeView()

        XCTAssertThrowsError(try view.inspect().find(text: String(localized: "cta_reenviar")))
    }

    func test_GIVEN_theInitialState_WHEN_rendered_THEN_noSubmitErrorIsShown() throws {
        let view = makeView()

        XCTAssertThrowsError(try view.inspect().find(text: "Código inválido ou expirado."))
    }
}
