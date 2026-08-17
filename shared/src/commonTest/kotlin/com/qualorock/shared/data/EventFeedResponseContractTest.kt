package com.qualorock.shared.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Fixture captured from a real `GET /api/events` response (api/app/Http/Resources/EventResource.php) — guards against contract drift. */
class EventFeedResponseContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun `given a real API feed response when decoding then it parses without error`() {
        val realResponseJson =
            """
            {"data":[{"id":"16","title":"Jean Felipe - Gravação de DVD","coverImageUrl":"http://localhost:9000/qor-local/events/covers/c076d890.jpg","startDateTime":"2026-08-17T19:30:00-03:00","venue":{"id":"5","name":"Matrix","imageUrl":null,"city":"Vitória","address":"Rio Branco","latitude":"-20.3103000","longitude":"-40.3211000","verificationStatus":"verified"},"city":"Vitória","price":{"isFree":true,"min":null,"max":null,"currency":"BRL"},"genres":["sertanejo"],"ticketUrl":"https://www.sympla.com.br/","status":"published"}],"next_cursor":"eyJzdGFydF9kYXRlX3RpbWUi"}
            """.trimIndent()

        val decoded = json.decodeFromString(EventFeedResponse.serializer(), realResponseJson)

        assertEquals(1, decoded.data.size)
        val event = decoded.data.first()
        assertEquals("16", event.id)
        assertEquals("Vitória", event.venue.city)
        assertEquals(-20.3103000, event.venue.latitude)
        assertTrue(event.price?.isFree == true)
        assertEquals("eyJzdGFydF9kYXRlX3RpbWUi", decoded.nextCursor)
    }
}
