import XCTest
import shared
@testable import iosApp

@MainActor
final class ProfileViewModelTests: XCTestCase {
    private func makeUser(
        name: String = "Ana Souza", email: String = "ana@example.com", phone: String? = "27999998888"
    ) -> User {
        User(
            id: 1,
            name: name,
            email: email,
            emailVerifiedAt: "2026-01-01T00:00:00Z",
            phone: phone,
            profilePictureUrl: nil,
            birthdate: "1998-05-10"
        )
    }

    func test_GIVEN_currentUser_WHEN_initialized_THEN_fieldsPrefilled() {
        let user = makeUser()
        let viewModel = ProfileViewModel(
            loadCurrentUser: { user },
            saveProfile: { _ in user },
            applySessionUser: { _ in }
        )

        XCTAssertEqual(viewModel.nameInput, user.name)
        XCTAssertEqual(viewModel.phoneInput, user.phone)
        XCTAssertEqual(viewModel.emailInput, user.email)
    }

    func test_GIVEN_validName_WHEN_saveName_THEN_appliesToSessionImmediately() async {
        let original = makeUser()
        let updated = makeUser(name: "Ana Beatriz Souza")
        var appliedUser: User?
        let viewModel = ProfileViewModel(
            loadCurrentUser: { original },
            saveProfile: { _ in updated },
            applySessionUser: { appliedUser = $0 }
        )
        viewModel.onNameChange("Ana Beatriz Souza")

        await viewModel.saveName()

        XCTAssertEqual(appliedUser?.name, "Ana Beatriz Souza")
        XCTAssertFalse(viewModel.nameError)
        XCTAssertFalse(viewModel.isSavingName)
    }

    func test_GIVEN_saveNameThrows_WHEN_saveName_THEN_nameErrorSet() async {
        struct Boom: Error {}
        let original = makeUser()
        let viewModel = ProfileViewModel(
            loadCurrentUser: { original },
            saveProfile: { _ in throw Boom() },
            applySessionUser: { _ in }
        )

        await viewModel.saveName()

        XCTAssertTrue(viewModel.nameError)
        XCTAssertFalse(viewModel.isSavingName)
    }

    func test_GIVEN_validEmail_WHEN_saveEmail_THEN_neverAppliesLocally_publishesPendingChange() async {
        let original = makeUser()
        var appliedUser: User?
        let viewModel = ProfileViewModel(
            loadCurrentUser: { original },
            saveProfile: { fields in
                XCTAssertEqual(fields.email, "novo@example.com")
                return original
            },
            applySessionUser: { appliedUser = $0 }
        )
        viewModel.onEmailChange("novo@example.com")

        await viewModel.saveEmail()

        XCTAssertNil(appliedUser, "AUTH-19: an email change must never apply to the session locally")
        XCTAssertEqual(viewModel.pendingEmailChange, "novo@example.com")
        XCTAssertEqual(viewModel.emailInput, original.email, "input resets to the still-current session email")
    }

    func test_GIVEN_saveEmailThrows_WHEN_saveEmail_THEN_emailErrorSet_noPendingChange() async {
        struct Boom: Error {}
        let original = makeUser()
        let viewModel = ProfileViewModel(
            loadCurrentUser: { original },
            saveProfile: { _ in throw Boom() },
            applySessionUser: { _ in }
        )
        viewModel.onEmailChange("novo@example.com")

        await viewModel.saveEmail()

        XCTAssertTrue(viewModel.emailError)
        XCTAssertNil(viewModel.pendingEmailChange)
    }

    func test_GIVEN_phoneSaveInFlight_WHEN_savePhone_THEN_isSavingPhoneTrueDuring_falseAfter() async {
        let original = makeUser()
        let viewModel = ProfileViewModel(
            loadCurrentUser: { original },
            saveProfile: { _ in original },
            applySessionUser: { _ in }
        )

        XCTAssertFalse(viewModel.isSavingPhone)
        await viewModel.savePhone()
        XCTAssertFalse(viewModel.isSavingPhone)
    }

    func test_GIVEN_fieldChange_THEN_correspondingErrorClears() {
        let original = makeUser()
        let viewModel = ProfileViewModel(
            loadCurrentUser: { original },
            saveProfile: { _ in original },
            applySessionUser: { _ in }
        )

        viewModel.onNameChange("x")
        viewModel.onPhoneChange("y")
        viewModel.onEmailChange("z")

        XCTAssertFalse(viewModel.nameError)
        XCTAssertFalse(viewModel.phoneError)
        XCTAssertFalse(viewModel.emailError)
    }
}
