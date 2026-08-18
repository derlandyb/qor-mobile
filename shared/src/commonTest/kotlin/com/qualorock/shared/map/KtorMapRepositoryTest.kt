package com.qualorock.shared.map

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

class KtorMapRepositoryTest {
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
    fun `given a successful API response when getMapMarkers is called then it returns the parsed events`() =
        runTest {
            val client =
                clientReturning(
                    HttpStatusCode.OK,
                    """{"data":[{"id":"16","title":"Jean Felipe","startDateTime":"2026-08-17T19:30:00-03:00",""" +
                        """"venue":{"id":"5","name":"Matrix","city":"Vitória","latitude":"-20.31","longitude":"-40.31",""" +
                        """"verificationStatus":"verified"},"city":"Vitória","genres":[],"status":"published"}]}""",
                )
            val repository = KtorMapRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getMapMarkers()

            assertTrue(result.isSuccess)
            val events = result.getOrThrow()
            assertEquals(1, events.size)
            assertEquals("16", events.first().id)
            // venue.latitude/longitude arrive as JSON strings from the real API — must still parse into Double.
            assertEquals(-20.31, events.first().venue.latitude)
            assertEquals(-40.31, events.first().venue.longitude)
        }

    @Test
    fun `given a network failure when getMapMarkers is called then it returns a failed Result`() =
        runTest {
            val engine = MockEngine { throw IllegalStateException("connection reset") }
            val client =
                HttpClient(engine) {
                    install(ContentNegotiation) { json() }
                }
            val repository = KtorMapRepository(baseUrl = "https://example.test", httpClient = client)

            val result = repository.getMapMarkers()

            assertFalse(result.isSuccess)
        }

    @Test
    fun `given filters when getMapMarkers is called then all filter params are sent as query parameters, with no cursor or q`() =
        runTest {
            var capturedUrl: Url? = null
            val engine =
                MockEngine { request ->
                    capturedUrl = request.url
                    respond(
                        content = """{"data":[]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
            val repository = KtorMapRepository(baseUrl = "https://example.test", httpClient = client)

            repository.getMapMarkers(
                dateBucket = DateBucket.FIM_DE_SEMANA,
                city = "Vila Velha",
                genres = listOf("Rock", "Samba"),
                artistId = "42",
            )

            val url = requireNotNull(capturedUrl)
            assertEquals("fim_de_semana", url.parameters["date_bucket"])
            assertEquals("Vila Velha", url.parameters["city"])
            assertEquals(listOf("Rock", "Samba"), url.parameters.getAll("genres[]"))
            assertEquals("42", url.parameters["artist_id"])
            assertNull(url.parameters["q"])
            assertNull(url.parameters["cursor"])
        }

    @Test
    fun `given a closed repository when getMapMarkers is called then it fails instead of leaking a request`() =
        runTest {
            val client = clientReturning(HttpStatusCode.OK, """{"data":[]}""")
            val repository = KtorMapRepository(baseUrl = "https://example.test", httpClient = client)

            repository.close()
            val result = repository.getMapMarkers()

            assertFalse(result.isSuccess)
        }
}
