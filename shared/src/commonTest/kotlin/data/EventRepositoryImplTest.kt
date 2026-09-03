package data

import domain.enum.City
import domain.event.EventDetail
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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

class EventRepositoryImplTest {

    private fun clientWithEngine(): Pair<HttpClient, MutableList<String>> {
        val capturedUrls = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedUrls.add(request.url.fullPath)
            respondFor(request.url.fullPath)
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
        return client to capturedUrls
    }

    private fun MockRequestHandleScope.respondFor(path: String) = when {
        path.contains("/events/evt-cancelled") -> respond(
            content = """{"data":${eventJson(id = "evt-cancelled", status = "cancelled")}}""",
            status = HttpStatusCode.OK,
            headers = jsonHeaders(),
        )
        path.contains("/events/evt-ended") -> respond(
            content = """{"data":${eventJson(id = "evt-ended", status = "ended")}}""",
            status = HttpStatusCode.OK,
            headers = jsonHeaders(),
        )
        path.contains("/events/evt-active") -> respond(
            content = """{"data":${eventJson(id = "evt-active", status = "published")}}""",
            status = HttpStatusCode.OK,
            headers = jsonHeaders(),
        )
        path.startsWith("/api/v1/events") -> respond(
            content = """{"data":[${eventJson(id = "evt-1", status = "published")}],"next_cursor":"cursor-2"}""",
            status = HttpStatusCode.OK,
            headers = jsonHeaders(),
        )
        else -> respond(content = "not found", status = HttpStatusCode.NotFound)
    }

    private fun jsonHeaders() = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }

    private fun eventJson(id: String, status: String) = """
        {
          "id": "$id",
          "title": "Show de Rock",
          "description": "Uma noite de rock",
          "cover_image_url": "https://cdn.example.com/$id.jpg",
          "starts_at": "2026-10-01T22:00:00Z",
          "city": "vitoria",
          "genre": "Rock",
          "address": "Rua das Flores, 100",
          "is_free": false,
          "ticket_url": "https://tickets.example.com/$id",
          "status": "$status",
          "promoters": []
        }
    """.trimIndent()

    @Test
    fun `GIVEN city genre and cursor filters WHEN findUpcoming is called THEN the request includes all three query params`() = runTest {
        val (client, capturedUrls) = clientWithEngine()
        val repository = EventRepositoryImpl(client, baseUrl = "http://test.local")

        repository.findUpcoming(city = City.Vitoria, genre = "Rock", cursor = "abc123")

        val url = capturedUrls.single()
        assertTrue(url.contains("city=vitoria"), "expected city=vitoria in $url")
        assertTrue(url.contains("genre=Rock"), "expected genre=Rock in $url")
        assertTrue(url.contains("cursor=abc123"), "expected cursor=abc123 in $url")
    }

    @Test
    fun `GIVEN no filters WHEN findUpcoming is called THEN the response maps to an EventPage with events and next cursor`() = runTest {
        val (client, _) = clientWithEngine()
        val repository = EventRepositoryImpl(client, baseUrl = "http://test.local")

        val page = repository.findUpcoming()

        assertEquals(1, page.events.size)
        assertEquals("evt-1", page.events.first().id)
        assertEquals("Show de Rock", page.events.first().title)
        assertEquals(City.Vitoria, page.events.first().city)
        assertEquals("cursor-2", page.nextCursor)
    }

    @Test
    fun `GIVEN an active published event WHEN findById is called THEN it maps to EventDetail Active`() = runTest {
        val (client, _) = clientWithEngine()
        val repository = EventRepositoryImpl(client, baseUrl = "http://test.local")

        val detail = repository.findById("evt-active")

        assertIs<EventDetail.Active>(detail)
    }

    @Test
    fun `GIVEN a cancelled event WHEN findById is called THEN it maps to EventDetail Cancelled not Active`() = runTest {
        val (client, _) = clientWithEngine()
        val repository = EventRepositoryImpl(client, baseUrl = "http://test.local")

        val detail = repository.findById("evt-cancelled")

        assertIs<EventDetail.Cancelled>(detail)
    }

    @Test
    fun `GIVEN an ended event WHEN findById is called THEN it maps to EventDetail Ended not Active`() = runTest {
        val (client, _) = clientWithEngine()
        val repository = EventRepositoryImpl(client, baseUrl = "http://test.local")

        val detail = repository.findById("evt-ended")

        assertIs<EventDetail.Ended>(detail)
    }
}
