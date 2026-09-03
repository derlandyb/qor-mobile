package domain.enum

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `qor-api`'s backed `ApprovalStatus` enum (ARCHITECTURE §14.1) — backs
 * `Venue.approval_status` / `Promoter.approval_status`. See [EventStatus] for the
 * no-catch-all/fail-loudly rule this file also follows.
 */
@Serializable
enum class ApprovalStatus {
    @SerialName("pending_approval")
    PendingApproval,

    @SerialName("approved")
    Approved,

    @SerialName("rejected")
    Rejected,

    @SerialName("suspended")
    Suspended,
}
