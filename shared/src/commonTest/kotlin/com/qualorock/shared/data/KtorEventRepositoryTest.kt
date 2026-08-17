package com.qualorock.shared.data

import com.qualorock.shared.filters.DateBucket
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun `given a query and filters when getEventFeed is called then all filter params are sent as query parameters`() =
        runTest {
            var capturedUrl: Url? = null
            val engine =
                MockEngine { request ->
                    capturedUrl = request.url
                    respond(
                        content = """{"data":[],"next_cursor":null}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
            val repository = KtorEventRepository(baseUrl = "https://example.test", httpClient = client)

            repository.getEventFeed(
                q = "forro",
                dateBucket = DateBucket.FIM_DE_SEMANA,
                city = "Vila Velha",
                genres = listOf("Rock", "Samba"),
                artistId = "42",
            )

            val url = requireNotNull(capturedUrl)
            assertEquals("forro", url.parameters["q"])
            assertEquals("fim_de_semana", url.parameters["date_bucket"])
            assertEquals("Vila Velha", url.parameters["city"])
            assertEquals(listOf("Rock", "Samba"), url.parameters.getAll("genres[]"))
            assertEquals("42", url.parameters["artist_id"])
        }

    @Test
    fun `given no filters when getEventFeed is called then no filter params are sent`() =
        runTest {
            var capturedUrl: Url? = null
            val engine =
                MockEngine { request ->
                    capturedUrl = request.url
                    respond(
                        content = """{"data":[],"next_cursor":null}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
            val repository = KtorEventRepository(baseUrl = "https://example.test", httpClient = client)

            repository.getEventFeed()

            val url = requireNotNull(capturedUrl)
            assertNull(url.parameters["q"])
            assertNull(url.parameters["date_bucket"])
            assertNull(url.parameters["city"])
            assertTrue(url.parameters.getAll("genres[]").isNullOrEmpty())
            assertNull(url.parameters["artist_id"])
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

    @Test
    fun `given a closed repository when getEventDetail is called then it fails instead of leaking a request`() =
        runTest {
            val client = clientReturning(HttpStatusCode.OK, """{"data":{}}""")
            val repository = KtorEventRepository(baseUrl = "https://example.test", httpClient = client)

            repository.close()
            val result = repository.getEventDetail("16")

            assertFalse(result.isSuccess)
        }
}
