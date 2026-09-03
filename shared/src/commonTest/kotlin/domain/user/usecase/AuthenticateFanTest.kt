package domain.user.usecase

import domain.user.LoginResult
import domain.user.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeSessionWriter : SessionWriter {
    var authenticatedUser: User? = null
    var authenticatedToken: String? = null

    override suspend fun onAuthenticated(user: User, token: String) {
        authenticatedUser = user
        authenticatedToken = token
    }
}

private fun sampleUser() = User(
    id = "u1",
    name = "Ana",
    email = "ana@example.com",
    emailVerifiedAt = "2026-01-01T00:00:00Z",
    phone = null,
    profilePictureUrl = null,
    birthdate = "1990-01-01",
)

class AuthenticateFanTest {

    @Test
    fun `GIVEN a successful login WHEN executeWithPassword is called THEN the session writer is notified`() = runTest {
        val repository = FakeUserRepository(loginResult = LoginResult.Success(sampleUser(), "tok-1"))
        val sessionWriter = FakeSessionWriter()
        val useCase = AuthenticateFan(repository, sessionWriter)

        useCase.executeWithPassword("ana@example.com", "correct-password")

        assertEquals(sampleUser(), sessionWriter.authenticatedUser)
        assertEquals("tok-1", sessionWriter.authenticatedToken)
    }

    @Test
    fun `GIVEN the server's exact pt-BR error message WHEN executeWithPassword fails THEN it passes through unmodified`() = runTest {
        val repository = FakeUserRepository(loginResult = LoginResult.InvalidCredentials("Credenciais inválidas."))
        val sessionWriter = FakeSessionWriter()
        val useCase = AuthenticateFan(repository, sessionWriter)

        val result = useCase.executeWithPassword("ana@example.com", "wrong-password")

        assertEquals(LoginResult.InvalidCredentials("Credenciais inválidas."), result)
        assertNull(sessionWriter.authenticatedUser)
    }
}
