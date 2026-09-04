package domain.user.usecase

import domain.user.User
import domain.user.VerifyEmailResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun sampleUser() = User(
    id = 1,
    name = "Ana",
    email = "ana@example.com",
    emailVerifiedAt = "2026-01-01T00:00:00Z",
    phone = null,
    profilePictureUrl = null,
    birthdate = "1990-01-01",
)

class VerifyEmailTest {

    @Test
    fun `GIVEN an email WHEN resend is called THEN it passes through unchanged to the repository`() = runTest {
        val repository = FakeUserRepository()
        val useCase = VerifyEmail(repository)

        useCase.resend("ana@example.com")

        assertEquals("ana@example.com", repository.resendVerificationEmail)
    }

    @Test
    fun `GIVEN a valid unexpired code WHEN verifyCode is called THEN the verified user passes through`() = runTest {
        val repository = FakeUserRepository(verifyEmailResult = VerifyEmailResult.Success(sampleUser()))
        val useCase = VerifyEmail(repository)

        val result = useCase.verifyCode("ana@example.com", "123456")

        assertEquals(VerifyEmailResult.Success(sampleUser()), result)
    }

    @Test
    fun `GIVEN an invalid code WHEN verifyCode is called THEN the server's exact pt-BR message passes through unmodified`() = runTest {
        val repository = FakeUserRepository(verifyEmailResult = VerifyEmailResult.Failure("Código inválido ou expirado."))
        val useCase = VerifyEmail(repository)

        val result = useCase.verifyCode("ana@example.com", "000000")

        assertEquals(VerifyEmailResult.Failure("Código inválido ou expirado."), result)
    }

    @Test
    fun `GIVEN an expired code WHEN verifyCode is called THEN the server's exact pt-BR message passes through unmodified`() = runTest {
        val repository = FakeUserRepository(verifyEmailResult = VerifyEmailResult.Failure("Código inválido ou expirado."))
        val useCase = VerifyEmail(repository)

        val result = useCase.verifyCode("ana@example.com", "999999")

        assertEquals(VerifyEmailResult.Failure("Código inválido ou expirado."), result)
    }
}
