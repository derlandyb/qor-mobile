package domain.user.usecase

import domain.user.ConfirmResetResult
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
    fun `GIVEN an expired token WHEN confirmReset is called THEN the server's exact message passes through unmodified`() = runTest {
        val repository = FakeUserRepository(confirmResetResult = ConfirmResetResult.Failure("Este link expirou."))
        val useCase = ResetPassword(repository)

        val result = useCase.confirmReset("expired-token", "N3wStr0ng!Pass")

        assertEquals(ConfirmResetResult.Failure("Este link expirou."), result)
    }
}
