package domain.enum

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CityTest {

    @Test
    fun `GIVEN the API raw value vitoria WHEN deserialized THEN it resolves to City Vitoria`() {
        val result = Json.decodeFromString(City.serializer(), "\"vitoria\"")
        assertEquals(City.Vitoria, result)
    }

    @Test
    fun `GIVEN the API raw value vila_velha WHEN deserialized THEN it resolves to City VilaVelha`() {
        val result = Json.decodeFromString(City.serializer(), "\"vila_velha\"")
        assertEquals(City.VilaVelha, result)
    }

    @Test
    fun `GIVEN the API raw value serra WHEN deserialized THEN it resolves to City Serra`() {
        val result = Json.decodeFromString(City.serializer(), "\"serra\"")
        assertEquals(City.Serra, result)
    }

    @Test
    fun `GIVEN the API raw value cariacica WHEN deserialized THEN it resolves to City Cariacica`() {
        val result = Json.decodeFromString(City.serializer(), "\"cariacica\"")
        assertEquals(City.Cariacica, result)
    }

    @Test
    fun `GIVEN an unknown raw value WHEN deserialized THEN it throws instead of silently defaulting`() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString(City.serializer(), "\"not_a_real_city\"")
        }
    }
}
