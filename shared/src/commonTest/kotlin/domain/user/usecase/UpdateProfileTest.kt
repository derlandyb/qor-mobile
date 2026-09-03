package domain.user.usecase

import domain.user.ProfileUpdateFields
import domain.user.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateProfileTest {

    @Test
    fun `GIVEN edited fields WHEN execute is called THEN they pass through unchanged to the repository`() = runTest {
        val updatedUser = User(
            id = "1",
            name = "Ana Nova",
            email = "ana@example.com",
            emailVerifiedAt = "2026-01-01T00:00:00Z",
            phone = "27999999999",
            profilePictureUrl = null,
            birthdate = "1995-05-05",
        )
        val repository = FakeUserRepository(updateProfileResult = updatedUser)
        val useCase = UpdateProfile(repository)
        val fields = ProfileUpdateFields(name = "Ana Nova", phone = "27999999999")

        val result = useCase.execute(fields)

        assertEquals(fields, repository.updatedFields)
        assertEquals(updatedUser, result)
    }
}
