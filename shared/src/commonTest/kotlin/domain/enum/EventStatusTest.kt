package domain.enum

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventStatusTest {

    @Test
    fun `GIVEN the API raw value draft WHEN deserialized THEN it resolves to EventStatus Draft`() {
        val result = Json.decodeFromString(EventStatus.serializer(), "\"draft\"")
        assertEquals(EventStatus.Draft, result)
    }

    @Test
    fun `GIVEN the API raw value pending_review WHEN deserialized THEN it resolves to EventStatus PendingReview`() {
        val result = Json.decodeFromString(EventStatus.serializer(), "\"pending_review\"")
        assertEquals(EventStatus.PendingReview, result)
    }

    @Test
    fun `GIVEN the API raw value published WHEN deserialized THEN it resolves to EventStatus Published`() {
        val result = Json.decodeFromString(EventStatus.serializer(), "\"published\"")
        assertEquals(EventStatus.Published, result)
    }

    @Test
    fun `GIVEN the API raw value cancelled WHEN deserialized THEN it resolves to EventStatus Cancelled`() {
        val result = Json.decodeFromString(EventStatus.serializer(), "\"cancelled\"")
        assertEquals(EventStatus.Cancelled, result)
    }

    @Test
    fun `GIVEN the API raw value ended WHEN deserialized THEN it resolves to EventStatus Ended`() {
        val result = Json.decodeFromString(EventStatus.serializer(), "\"ended\"")
        assertEquals(EventStatus.Ended, result)
    }

    @Test
    fun `GIVEN an unknown raw value WHEN deserialized THEN it throws instead of silently defaulting`() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString(EventStatus.serializer(), "\"not_a_real_status\"")
        }
    }
}
