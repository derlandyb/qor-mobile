package domain.enum

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApprovalStatusTest {

    @Test
    fun `GIVEN the API raw value pending_approval WHEN deserialized THEN it resolves to ApprovalStatus PendingApproval`() {
        val result = Json.decodeFromString(ApprovalStatus.serializer(), "\"pending_approval\"")
        assertEquals(ApprovalStatus.PendingApproval, result)
    }

    @Test
    fun `GIVEN the API raw value approved WHEN deserialized THEN it resolves to ApprovalStatus Approved`() {
        val result = Json.decodeFromString(ApprovalStatus.serializer(), "\"approved\"")
        assertEquals(ApprovalStatus.Approved, result)
    }

    @Test
    fun `GIVEN the API raw value rejected WHEN deserialized THEN it resolves to ApprovalStatus Rejected`() {
        val result = Json.decodeFromString(ApprovalStatus.serializer(), "\"rejected\"")
        assertEquals(ApprovalStatus.Rejected, result)
    }

    @Test
    fun `GIVEN the API raw value suspended WHEN deserialized THEN it resolves to ApprovalStatus Suspended`() {
        val result = Json.decodeFromString(ApprovalStatus.serializer(), "\"suspended\"")
        assertEquals(ApprovalStatus.Suspended, result)
    }

    @Test
    fun `GIVEN an unknown raw value WHEN deserialized THEN it throws instead of silently defaulting`() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString(ApprovalStatus.serializer(), "\"not_a_real_status\"")
        }
    }
}
