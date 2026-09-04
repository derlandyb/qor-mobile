package domain.user.usecase

import domain.user.ConfirmResetResult
import domain.user.VerifyResetCodeResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ResetPasswordTest {

    @Test
    fun `GIVEN an email WHEN requestReset is called THEN it passes through unchanged to the repository`() = runTest {
        val repository = FakeUserRepository()
        val useCase = ResetPassword(repository)

        useCase.requestReset("ana@example.com")

        assertEquals("ana@example.com", repository.requestedResetEmail)
    }

    @Test
    fun `GIVEN a valid unexpired code WHEN verifyResetCode is called THEN the reset token passes through`() = runTest {
        val repository = FakeUserRepository(verifyResetCodeResult = VerifyResetCodeResult.Success("reset-token-123"))
        val useCase = ResetPassword(repository)

        val result = useCase.verifyResetCode("ana@example.com", "123456")

        assertEquals(VerifyResetCodeResult.Success("reset-token-123"), result)
    }

    @Test
    fun `GIVEN an invalid code WHEN verifyResetCode is called THEN the server's exact message passes through unmodified`() = runTest {
        val repository = FakeUserRepository(verifyResetCodeResult = VerifyResetCodeResult.Failure("Código inválido ou expirado."))
        val useCase = ResetPassword(repository)

        val result = useCase.verifyResetCode("ana@example.com", "000000")

        assertEquals(VerifyResetCodeResult.Failure("Código inválido ou expirado."), result)
    }

    @Test
    fun `GIVEN an expired token WHEN confirmReset is called THEN the server's exact message passes through unmodified`() = runTest {
        val repository = FakeUserRepository(confirmResetResult = ConfirmResetResult.Failure("Este link expirou."))
        val useCase = ResetPassword(repository)

        val result = useCase.confirmReset("ana@example.com", "expired-token", "N3wStr0ng!Pass")

        assertEquals(ConfirmResetResult.Failure("Este link expirou."), result)
    }
}
