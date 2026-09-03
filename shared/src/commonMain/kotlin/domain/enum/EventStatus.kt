package domain.enum

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `qor-api`'s backed `EventStatus` enum (ARCHITECTURE §14.1). Raw values match the
 * API's snake_case backed-enum convention exactly — a cross-file contract with `api.md` T4/T23.
 *
 * No catch-all/`Unknown` case: an unrecognized raw value is a contract break between client and
 * server, and must fail loudly (throw) rather than silently default — this is
 * `kotlinx.serialization`'s default enum-deserialization behavior, left un-suppressed.
 */
@Serializable
enum class EventStatus {
    @SerialName("draft")
    Draft,

    @SerialName("pending_review")
    PendingReview,

    @SerialName("published")
    Published,

    @SerialName("cancelled")
    Cancelled,

    @SerialName("ended")
    Ended,
}
