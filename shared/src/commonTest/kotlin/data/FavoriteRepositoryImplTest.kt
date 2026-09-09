package data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class FavoriteRepositoryImplTest {

    private fun jsonHeaders() = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }

    private fun clientReturning(status: HttpStatusCode, body: String): Pair<HttpClient, MutableList<String>> {
        val capturedUrls = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedUrls.add(request.url.fullPath)
            respond(content = body, status = status, headers = jsonHeaders())
        }
        // Mirrors production's `qorJson` (data/QorHttpClient.kt): ignoreUnknownKeys = true,
        // since the real favorites payload carries `genre_id`, a field this DTO doesn't model.
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return client to capturedUrls
    }

    @Test
    fun `GIVEN a successful toggle response WHEN toggle is called THEN the new favorited state returns`() = runTest {
        val (client, capturedUrls) = clientReturning(
            HttpStatusCode.OK,
            """{"data":{"event_id":42,"favorited":true}}""",
        )
        val repository = FavoriteRepositoryImpl(client, baseUrl = "http://test.local")

        val favorited = repository.toggle("42")

        assertTrue(favorited)
        assertTrue(capturedUrls.single().endsWith("/events/42/favorite"), capturedUrls.single())
    }

    @Test
    fun `GIVEN a toggle-off response WHEN toggle is called THEN the new unfavorited state returns`() = runTest {
        val (client, _) = clientReturning(
            HttpStatusCode.OK,
            """{"data":{"event_id":42,"favorited":false}}""",
        )
        val repository = FavoriteRepositoryImpl(client, baseUrl = "http://test.local")

        val favorited = repository.toggle("42")

        assertEquals(false, favorited)
    }

    @Test
    fun `GIVEN a list response shaped like the real favorites endpoint WHEN list is called THEN it maps to Event correctly`() = runTest {
        // Mirrors FavoriteController::eventToArray's real shape: no `genre` key (only
        // `genre_id`, which the shared domain layer doesn't model), no latitude/longitude.
        val (client, capturedUrls) = clientReturning(
            HttpStatusCode.OK,
            """
            {
              "data": [
                {
                  "id": 7,
                  "title": "Show de Rock",
                  "description": "Uma noite de rock",
                  "cover_image_url": null,
                  "starts_at": "2026-10-01T22:00:00Z",
                  "city": "vitoria",
                  "genre_id": 3,
                  "address": "Rua das Flores, 100",
                  "is_free": false,
                  "ticket_url": null,
                  "status": "published"
                }
              ],
              "next_cursor": null
            }
            """.trimIndent(),
        )
        val repository = FavoriteRepositoryImpl(client, baseUrl = "http://test.local")

        val events = repository.list()

        assertEquals(1, events.size)
        assertEquals("7", events.first().id)
        assertEquals("Show de Rock", events.first().title)
        assertTrue(capturedUrls.single().endsWith("/profile/favorites"), capturedUrls.single())
    }

    @Test
    fun `GIVEN an empty favorites list WHEN list is called THEN it returns an empty list`() = runTest {
        val (client, _) = clientReturning(HttpStatusCode.OK, """{"data":[],"next_cursor":null}""")
        val repository = FavoriteRepositoryImpl(client, baseUrl = "http://test.local")

        val events = repository.list()

        assertTrue(events.isEmpty())
    }

    @Test
    fun `GIVEN an auth error response WHEN toggle is called THEN it fails rather than returning a bogus state`() = runTest {
        val (client, _) = clientReturning(HttpStatusCode.Unauthorized, """{"message":"Unauthenticated."}""")
        val repository = FavoriteRepositoryImpl(client, baseUrl = "http://test.local")

        assertFails { repository.toggle("42") }
    }

    @Test
    fun `GIVEN a network error response WHEN list is called THEN it fails rather than returning a bogus list`() = runTest {
        val (client, _) = clientReturning(HttpStatusCode.InternalServerError, """{"message":"Erro interno."}""")
        val repository = FavoriteRepositoryImpl(client, baseUrl = "http://test.local")

        assertFails { repository.list() }
    }
}
