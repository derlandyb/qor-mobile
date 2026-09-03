package domain.enum

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `qor-api`'s backed `ConsentType` enum (ARCHITECTURE §14.1) — backs
 * `ConsentRecord.consentType` (`auth-fan-profile/design.md`). See [EventStatus] for the
 * no-catch-all/fail-loudly rule this file also follows.
 */
@Serializable
enum class ConsentType {
    @SerialName("terms")
    Terms,

    @SerialName("location")
    Location,
}
