package com.qualorock.shared.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EventStatus {
    @SerialName("draft")
    DRAFT,

    @SerialName("pending_approval")
    PENDING_APPROVAL,

    @SerialName("approved")
    APPROVED,

    @SerialName("published")
    PUBLISHED,

    @SerialName("changes_requested")
    CHANGES_REQUESTED,

    @SerialName("cancelled")
    CANCELLED,

    @SerialName("finished")
    FINISHED,
}

@Serializable
enum class VerificationStatus {
    @SerialName("unverified")
    UNVERIFIED,

    @SerialName("pending_review")
    PENDING_REVIEW,

    @SerialName("verified")
    VERIFIED,
}

@Serializable
enum class AgeRating {
    @SerialName("L")
    L,

    @SerialName("10")
    TEN,

    @SerialName("12")
    TWELVE,

    @SerialName("14")
    FOURTEEN,

    @SerialName("16")
    SIXTEEN,

    @SerialName("18")
    EIGHTEEN,
}
