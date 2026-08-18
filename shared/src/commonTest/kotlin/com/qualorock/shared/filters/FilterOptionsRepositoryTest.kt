package com.qualorock.shared.filters

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilterOptionsRepositoryTest {
    private fun clientReturning(
        status: HttpStatusCode,
        body: String,
    ): HttpClient {
        val engine =
            MockEngine { _ ->
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        return HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `given a successful API response when getGenreOptions is called then it returns the parsed genre list`() =
        runTest {
            val client = clientReturning(HttpStatusCode.OK, """{"data":["Rock","Samba"]}""")
            val repository = KtorFilterOptionsRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getGenreOptions()

            assertTrue(result.isSuccess)
            assertEquals(listOf("Rock", "Samba"), result.getOrThrow())
        }

    @Test
    fun `given a network failure when getGenreOptions is called then it returns a failed Result`() =
        runTest {
            val engine = MockEngine { throw IllegalStateException("connection reset") }
            val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
            val repository = KtorFilterOptionsRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getGenreOptions()

            assertFalse(result.isSuccess)
        }

    @Test
    fun `given a successful API response when getArtistOptions is called then it returns the parsed artist list`() =
        runTest {
            val client =
                clientReturning(
                    HttpStatusCode.OK,
                    """{"data":[{"id":"1","name":"Jorge & the Band"}]}""",
                )
            val repository = KtorFilterOptionsRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getArtistOptions()

            assertTrue(result.isSuccess)
            assertEquals(listOf(ArtistOption(id = "1", name = "Jorge & the Band")), result.getOrThrow())
        }

    @Test
    fun `given a network failure when getArtistOptions is called then it returns a failed Result`() =
        runTest {
            val engine = MockEngine { throw IllegalStateException("connection reset") }
            val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
            val repository = KtorFilterOptionsRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getArtistOptions()

            assertFalse(result.isSuccess)
        }
}
