package domain.enum

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConsentTypeTest {

    @Test
    fun `GIVEN the API raw value terms WHEN deserialized THEN it resolves to ConsentType Terms`() {
        val result = Json.decodeFromString(ConsentType.serializer(), "\"terms\"")
        assertEquals(ConsentType.Terms, result)
    }

    @Test
    fun `GIVEN the API raw value location WHEN deserialized THEN it resolves to ConsentType Location`() {
        val result = Json.decodeFromString(ConsentType.serializer(), "\"location\"")
        assertEquals(ConsentType.Location, result)
    }

    @Test
    fun `GIVEN an unknown raw value WHEN deserialized THEN it throws instead of silently defaulting`() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString(ConsentType.serializer(), "\"not_a_real_consent\"")
        }
    }
}
