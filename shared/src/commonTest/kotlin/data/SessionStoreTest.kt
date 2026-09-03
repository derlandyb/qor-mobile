package data

import domain.enum.ConsentType
import domain.user.ConfirmResetResult
import domain.user.DataRightResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import domain.user.User
import domain.user.UserRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun sampleUser() = User(
    id = "u1",
    name = "Ana",
    email = "ana@example.com",
    emailVerifiedAt = "2026-01-01T00:00:00Z",
    phone = null,
    profilePictureUrl = null,
    birthdate = "1990-01-01",
)

private class FakeUserRepository(private val profile: User) : UserRepository {
    override suspend fun register(email: String, password: String, birthdate: String, name: String, consentAccepted: Boolean): RegisterResult =
        error("not used")
    override suspend fun login(email: String, password: String): LoginResult = error("not used")
    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult = error("not used")
    override suspend fun logout() = Unit
    override suspend fun requestPasswordReset(email: String) = Unit
    override suspend fun confirmPasswordReset(token: String, newPassword: String): ConfirmResetResult = error("not used")
    override suspend fun resendVerification(email: String) = Unit
    override suspend fun verifyEmailCode(email: String, code: String): domain.user.VerifyEmailResult = error("not used")
    override suspend fun getProfile(): User = profile
    override suspend fun updateProfile(fields: ProfileUpdateFields): User = error("not used")
    override suspend fun accessData(): DataRightResult = error("not used")
    override suspend fun exportData(): DataRightResult = error("not used")
    override suspend fun deleteAccount(): DataRightResult = error("not used")
    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult = error("not used")
}

class SessionStoreTest {

    @Test
    fun `GIVEN a stored token WHEN restore is called THEN currentUser is populated from the profile`() = runTest {
        val tokenStorage = FakeSecureTokenStorage(initialToken = "tok-1")
        val store = SessionStore(FakeUserRepository(sampleUser()), tokenStorage)

        store.restore()

        assertEquals(sampleUser(), store.currentUser.value)
    }

    @Test
    fun `GIVEN no stored token WHEN restore is called THEN currentUser stays null`() = runTest {
        val tokenStorage = FakeSecureTokenStorage(initialToken = null)
        val store = SessionStore(FakeUserRepository(sampleUser()), tokenStorage)

        store.restore()

        assertNull(store.currentUser.value)
    }

    @Test
    fun `GIVEN a user and token WHEN set is called THEN currentUser emits the user and the token is written to storage`() = runTest {
        val tokenStorage = FakeSecureTokenStorage()
        val store = SessionStore(FakeUserRepository(sampleUser()), tokenStorage)

        store.set(sampleUser(), "tok-2")

        assertEquals(sampleUser(), store.currentUser.value)
        assertEquals("tok-2", tokenStorage.read())
    }

    @Test
    fun `GIVEN a logged-in session WHEN clear is called THEN currentUser becomes null and storage is cleared`() = runTest {
        val tokenStorage = FakeSecureTokenStorage(initialToken = "tok-3")
        val store = SessionStore(FakeUserRepository(sampleUser()), tokenStorage)
        store.set(sampleUser(), "tok-3")

        store.clear()

        assertNull(store.currentUser.value)
        assertNull(tokenStorage.read())
    }

    @Test
    fun `GIVEN a successful login WHEN onAuthenticated is called THEN the token is persisted to secure storage`() = runTest {
        val tokenStorage = FakeSecureTokenStorage()
        val store = SessionStore(FakeUserRepository(sampleUser()), tokenStorage)

        store.onAuthenticated(sampleUser(), "tok-4")

        assertEquals(sampleUser(), store.currentUser.value)
        assertEquals("tok-4", tokenStorage.read())
    }

    @Test
    fun `GIVEN a logged-in session WHEN updateCurrentUser is called THEN currentUser reflects the new value without touching stored token`() =
        runTest {
            val tokenStorage = FakeSecureTokenStorage(initialToken = "tok-5")
            val store = SessionStore(FakeUserRepository(sampleUser()), tokenStorage)
            store.set(sampleUser(), "tok-5")
            val renamed = sampleUser().copy(name = "Ana Renomeada")

            store.updateCurrentUser(renamed)

            assertEquals(renamed, store.currentUser.value)
            assertEquals("tok-5", tokenStorage.read())
        }
}
