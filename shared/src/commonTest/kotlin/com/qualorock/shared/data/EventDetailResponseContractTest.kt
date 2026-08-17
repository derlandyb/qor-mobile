package com.qualorock.shared.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fixture captured from a real `GET /api/events/{id}` response
 * (api/app/Http/Resources/EventDetailResource.php) — guards against contract drift.
 */
class EventDetailResponseContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun `given a real API detail response when decoding then it parses without error`() {
        val realResponseJson =
            """
            {"data":{"id":"16","title":"Jean Felipe - Gravação de DVD","description":"Uma noite especial de rock.","coverImageUrl":"http://localhost:9000/qor-local/events/covers/c076d890.jpg","startDateTime":"2026-08-17T19:30:00-03:00","endDateTime":"2026-08-17T23:00:00-03:00","venue":{"id":"5","name":"Matrix","imageUrl":null,"description":null,"address":"Rio Branco","city":"Vitória","latitude":"-20.3103000","longitude":"-40.3211000","staticMapUrl":"https://maps.googleapis.com/maps/api/staticmap?center=-20.3103000,-40.3211000","contactPhone":"+55 27 99999-0000","contactEmail":"contato@matrix.com","socialLinks":{"instagram":"https://instagram.com/matrix"},"verificationStatus":"verified"},"city":"Vitória","price":{"isFree":true,"min":null,"max":null,"currency":"BRL"},"ageRating":"18","genres":["sertanejo"],"ticketUrl":"https://www.sympla.com.br/","status":"published","bannerStatus":null,"promoter":{"id":"9","name":"Produtora XYZ","imageUrl":null,"description":"Produtora de eventos.","socialLinks":{"instagram":"https://instagram.com/xyz","whatsapp":"https://wa.me/5527999990000"},"contactPhone":"+55 27 98888-0000","contactEmail":"contato@xyz.com","verificationStatus":"verified"}}}
            """.trimIndent()

        val decoded = json.decodeFromString(EventDetailEnvelope.serializer(), realResponseJson)

        val event = decoded.data
        assertEquals("16", event.id)
        assertEquals("Uma noite especial de rock.", event.description)
        assertEquals("Vitória", event.venue.city)
        assertEquals(-20.3103000, event.venue.latitude)
        assertTrue(event.venue.staticMapUrl?.startsWith("https://maps.googleapis.com") == true)
        assertNull(event.bannerStatus)
        assertEquals("Produtora XYZ", event.promoter?.name)
        assertEquals("https://wa.me/5527999990000", event.promoter?.socialLinks?.get("whatsapp"))
    }

    @Test
    fun `given a cancelled event detail response when decoding then ticketUrl is absent and bannerStatus is cancelled`() {
        val realResponseJson =
            """
            {"data":{"id":"20","title":"Show Cancelado","description":null,"coverImageUrl":null,"startDateTime":"2026-09-01T20:00:00-03:00","venue":{"id":"6","name":"Cine Theatro","imageUrl":null,"description":null,"address":null,"city":"Vitória","latitude":null,"longitude":null,"verificationStatus":"unverified"},"city":"Vitória","genres":[],"status":"cancelled","bannerStatus":"cancelled"}}
            """.trimIndent()

        val decoded = json.decodeFromString(EventDetailEnvelope.serializer(), realResponseJson)

        val event = decoded.data
        assertEquals(com.qualorock.shared.domain.BannerStatus.CANCELLED, event.bannerStatus)
        assertNull(event.ticketUrl)
        assertNull(event.venue.staticMapUrl)
    }
}
