package domain.user.usecase

import domain.user.RegisterResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RegisterFanTest {

    @Test
    fun `GIVEN a duplicate email WHEN execute is called THEN the server's exact pt-BR message passes through unmodified`() = runTest {
        val repository = FakeUserRepository(
            registerResult = RegisterResult.Failure(
                "Dados inválidos.",
                mapOf("email" to listOf("Este e-mail já está em uso.")),
            ),
        )
        val useCase = RegisterFan(repository)

        val result = useCase.execute("ana@example.com", "Str0ng!Pass", "1990-01-01", "Ana", true)

        assertEquals(
            RegisterResult.Failure("Dados inválidos.", mapOf("email" to listOf("Este e-mail já está em uso."))),
            result,
        )
    }
}
