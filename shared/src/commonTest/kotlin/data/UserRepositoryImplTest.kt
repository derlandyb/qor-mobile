package data

import domain.user.ConfirmResetResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.content.TextContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UserRepositoryImplTest {

    private fun jsonHeaders() = Headers.build { append(HttpHeaders.ContentType, "application/json") }

    private fun clientWith(
        capturedRequests: MutableList<HttpRequestData> = mutableListOf(),
        handler: (HttpRequestData) -> Pair<HttpStatusCode, String>,
    ): Pair<HttpClient, MutableList<HttpRequestData>> {
        val engine = MockEngine { request ->
            capturedRequests.add(request)
            val (status, body) = handler(request)
            respond(content = body, status = status, headers = jsonHeaders())
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
        return client to capturedRequests
    }

    @Test
    fun `GIVEN valid credentials WHEN login is called THEN it builds a POST to auth login and maps the session`() = runTest {
        val (client, requests) = clientWith { request ->
            HttpStatusCode.OK to """{"data":{"id":"u1","name":"Ana","email":"ana@example.com","email_verified_at":"2026-01-01T00:00:00Z","birthdate":"1990-01-01"},"token":"tok-123"}"""
        }
        val repository = UserRepositoryImpl(client, baseUrl = "http://test.local")

        val result = repository.login("ana@example.com", "correct-password")

        assertIs<LoginResult.Success>(result)
        assertEquals("tok-123", result.token)
        assertEquals("ana@example.com", result.user.email)
        val request = requests.single()
        assertTrue(request.url.fullPath.endsWith("/auth/login"))
        assertTrue((request.body as TextContent).text.contains("ana@example.com"))
    }

    @Test
    fun `GIVEN a wrong password WHEN login is called THEN the server's generic pt-BR message passes through unmodified`() = runTest {
        val (client, _) = clientWith {
            HttpStatusCode.Unauthorized to """{"message":"Credenciais inválidas."}"""
        }
        val repository = UserRepositoryImpl(client, baseUrl = "http://test.local")

        val result = repository.login("ana@example.com", "wrong-password")

        assertIs<LoginResult.InvalidCredentials>(result)
        assertEquals("Credenciais inválidas.", result.message)
    }

    @Test
    fun `GIVEN an unverified account WHEN login is called THEN it maps to UnverifiedAccount not InvalidCredentials`() = runTest {
        val (client, _) = clientWith {
            HttpStatusCode.Forbidden to """{"message":"Confirme seu e-mail para continuar."}"""
        }
        val repository = UserRepositoryImpl(client, baseUrl = "http://test.local")

        val result = repository.login("ana@example.com", "correct-password")

        assertIs<LoginResult.UnverifiedAccount>(result)
        assertEquals("Confirme seu e-mail para continuar.", result.message)
    }

    @Test
    fun `GIVEN a duplicate email WHEN register is called THEN the server's field error passes through`() = runTest {
        val (client, _) = clientWith {
            HttpStatusCode.UnprocessableEntity to
                """{"message":"Dados inválidos.","errors":{"email":["Este e-mail já está em uso."]}}"""
        }
        val repository = UserRepositoryImpl(client, baseUrl = "http://test.local")

        val result = repository.register(
            email = "ana@example.com",
            password = "Str0ng!Pass",
            birthdate = "1990-01-01",
            name = "Ana",
            consentAccepted = true,
        )

        assertIs<RegisterResult.Failure>(result)
        assertEquals(listOf("Este e-mail já está em uso."), result.fieldErrors["email"])
    }

    @Test
    fun `GIVEN a known token WHEN getProfile is called THEN it builds a GET to profile and maps the user`() = runTest {
        val (client, requests) = clientWith {
            HttpStatusCode.OK to """{"data":{"id":"u1","name":"Ana","email":"ana@example.com","birthdate":"1990-01-01"}}"""
        }
        val repository = UserRepositoryImpl(client, baseUrl = "http://test.local")

        val user = repository.getProfile()

        assertEquals("Ana", user.name)
        assertTrue(requests.single().url.fullPath.endsWith("/profile"))
    }

    @Test
    fun `GIVEN updated fields WHEN updateProfile is called THEN it builds a PATCH and maps the updated user`() = runTest {
        val (client, requests) = clientWith {
            HttpStatusCode.OK to """{"data":{"id":"u1","name":"Ana Paula","email":"ana@example.com","birthdate":"1990-01-01"}}"""
        }
        val repository = UserRepositoryImpl(client, baseUrl = "http://test.local")

        val user = repository.updateProfile(ProfileUpdateFields(name = "Ana Paula"))

        assertEquals("Ana Paula", user.name)
        assertEquals("PATCH", requests.single().method.value)
    }

    @Test
    fun `GIVEN a valid unexpired token WHEN confirmPasswordReset is called THEN it succeeds`() = runTest {
        val (client, _) = clientWith { HttpStatusCode.OK to """{"message":"ok"}""" }
        val repository = UserRepositoryImpl(client, baseUrl = "http://test.local")

        val result = repository.confirmPasswordReset("valid-token", "N3wStr0ng!Pass")

        assertIs<ConfirmResetResult.Success>(result)
    }

    @Test
    fun `GIVEN an expired token WHEN confirmPasswordReset is called THEN the failure message passes through`() = runTest {
        val (client, _) = clientWith {
            HttpStatusCode.UnprocessableEntity to """{"message":"Este link expirou."}"""
        }
        val repository = UserRepositoryImpl(client, baseUrl = "http://test.local")

        val result = repository.confirmPasswordReset("expired-token", "N3wStr0ng!Pass")

        assertIs<ConfirmResetResult.Failure>(result)
        assertEquals("Este link expirou.", result.message)
    }
}
