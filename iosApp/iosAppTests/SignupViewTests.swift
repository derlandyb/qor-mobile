import XCTest
import ViewInspector
import shared
@testable import iosApp

private struct FakeSignupRegistering: SignupRegistering {
    func execute(
        email: String, password: String, birthdate: String, name: String, consentAccepted: Bool
    ) async throws -> RegisterResult {
        RegisterResult.Success(user: User(
            id: 1, name: name, email: email, emailVerifiedAt: nil,
            phone: nil, profilePictureUrl: nil, birthdate: birthdate
        ))
    }
}

@MainActor
final class SignupViewTests: XCTestCase {
    private func makeSut() -> SignupView {
        let viewModel = SignupViewModel(registerFan: FakeSignupRegistering(), onSignupSuccess: { _ in })
        return SignupView(viewModel: viewModel, onNavigateToLogin: {})
    }

    func test_GIVEN_freshSignupView_WHEN_rendered_THEN_everyFormFieldAndActionIsPresent() throws {
        let sut = makeSut()

        XCTAssertNoThrow(try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_name_field"))
        XCTAssertNoThrow(try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_email_field"))
        XCTAssertNoThrow(try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_password_field"))
        XCTAssertNoThrow(try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_confirm_password_field"))
        XCTAssertNoThrow(try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_birthdate_field"))
        XCTAssertNoThrow(try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_submit_button"))
        XCTAssertNoThrow(try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_google_button"))
        XCTAssertNoThrow(try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_login_link"))
    }

    func test_GIVEN_consentNotAccepted_WHEN_rendered_THEN_submitButtonIsDisabled() throws {
        let sut = makeSut()

        let button = try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_submit_button")
        XCTAssertTrue(try button.button().isDisabled())
    }

    func test_GIVEN_googleSignupButton_WHEN_rendered_THEN_itIsDisabled() throws {
        let sut = makeSut()

        let button = try sut.inspect().find(viewWithAccessibilityIdentifier: "signup_google_button")
        XCTAssertTrue(try button.button().isDisabled())
    }
}
