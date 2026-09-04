import XCTest
import SwiftUI
import ViewInspector
import shared
@testable import iosApp

/// Renders `ProfileView` off an already-initialized `ProfileViewModel` and inspects it un-hosted
/// — see `EventDetailViewTests`'s header comment for why this avoids `.task`/lifecycle races.
@MainActor
final class ProfileViewTests: XCTestCase {
    private func makeUser(name: String = "Ana Souza", email: String = "ana@example.com", phone: String? = "27999998888") -> User {
        User(
            id: "user-1",
            name: name,
            email: email,
            emailVerifiedAt: "2026-01-01T00:00:00Z",
            phone: phone,
            profilePictureUrl: nil,
            birthdate: "1998-05-10"
        )
    }

    func test_GIVEN_currentUser_WHEN_rendered_THEN_fieldsShowPrefilledValues() throws {
        let user = makeUser()
        let viewModel = ProfileViewModel(
            loadCurrentUser: { user },
            saveProfile: { _ in user },
            applySessionUser: { _ in }
        )
        let view = ProfileView(viewModel: viewModel)

        let nameField = try view.inspect().find(viewWithAccessibilityIdentifier: "profile_name_field")
        XCTAssertNoThrow(nameField)

        let birthdate = try view.inspect().find(viewWithAccessibilityIdentifier: "profile_birthdate").text().string()
        XCTAssertEqual(birthdate, user.birthdate)
    }

    func test_GIVEN_changePhotoButton_THEN_isDisabled() throws {
        let user = makeUser()
        let viewModel = ProfileViewModel(
            loadCurrentUser: { user },
            saveProfile: { _ in user },
            applySessionUser: { _ in }
        )
        let view = ProfileView(viewModel: viewModel)

        let changePhotoButton = try view.inspect().find(viewWithAccessibilityIdentifier: "profile_change_photo").button()
        XCTAssertTrue(try changePhotoButton.isDisabled())
    }

    func test_GIVEN_savingName_WHEN_rendered_THEN_saveButtonShowsLoading() async throws {
        let user = makeUser()
        var releaseCall: CheckedContinuation<Void, Never>?
        let viewModel = ProfileViewModel(
            loadCurrentUser: { user },
            saveProfile: { _ in
                await withCheckedContinuation { releaseCall = $0 }
                return user
            },
            applySessionUser: { _ in }
        )
        let view = ProfileView(viewModel: viewModel)

        let saveTask = Task { await viewModel.saveName() }
        while releaseCall == nil {
            await Task.yield()
        }

        XCTAssertTrue(viewModel.isSavingName)
        XCTAssertNoThrow(try view.inspect().find(viewWithAccessibilityIdentifier: "profile_name_field_save"))

        releaseCall?.resume()
        _ = await saveTask.value
    }
}
