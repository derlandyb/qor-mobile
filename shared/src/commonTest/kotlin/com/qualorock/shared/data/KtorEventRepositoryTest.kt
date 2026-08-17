package com.qualorock.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KtorEventRepositoryTest {
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
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }
    }

    @Test
    fun `given a successful API response when getEventFeed is called then it returns the parsed page`() =
        runTest {
            val client =
                clientReturning(
                    HttpStatusCode.OK,
                    """{"data":[],"next_cursor":"abc123"}""",
                )
            val repository = KtorEventRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getEventFeed()

            assertTrue(result.isSuccess)
            assertEquals("abc123", result.getOrThrow().nextCursor)
            assertTrue(result.getOrThrow().events.isEmpty())
        }

    @Test
    fun `given a network failure when getEventFeed is called then it returns a failed Result`() =
        runTest {
            val engine = MockEngine { throw IllegalStateException("connection reset") }
            val client =
                HttpClient(engine) {
                    install(ContentNegotiation) { json() }
                }
            val repository = KtorEventRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getEventFeed()

            assertFalse(result.isSuccess)
        }

    @Test
    fun `given a successful API response when getEventDetail is called then it returns the parsed event`() =
        runTest {
            val client =
                clientReturning(
                    HttpStatusCode.OK,
                    """{"data":{"id":"16","title":"Jean Felipe","startDateTime":"2026-08-17T19:30:00-03:00",""" +
                        """"venue":{"id":"5","name":"Matrix","city":"Vitória","verificationStatus":"verified"},""" +
                        """"city":"Vitória","genres":[],"status":"published"}}""",
                )
            val repository = KtorEventRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getEventDetail("16")

            assertTrue(result.isSuccess)
            assertEquals("16", result.getOrThrow().id)
        }

    @Test
    fun `given a 404 response when getEventDetail is called then it fails with EventNotFoundException`() =
        runTest {
            val client = clientReturning(HttpStatusCode.NotFound, """{"message":"Evento não encontrado."}""")
            val repository = KtorEventRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getEventDetail("missing")

            assertFalse(result.isSuccess)
            assertTrue(result.exceptionOrNull() is EventNotFoundException)
            assertEquals("missing", (result.exceptionOrNull() as EventNotFoundException).eventId)
        }

    @Test
    fun `given a network failure when getEventDetail is called then it returns a failed Result`() =
        runTest {
            val engine = MockEngine { throw IllegalStateException("connection reset") }
            val client =
                HttpClient(engine) {
                    install(ContentNegotiation) { json() }
                }
            val repository = KtorEventRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getEventDetail("16")

            assertFalse(result.isSuccess)
        }
}
