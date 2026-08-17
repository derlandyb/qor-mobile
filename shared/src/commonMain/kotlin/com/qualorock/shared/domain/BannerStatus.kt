package com.qualorock.shared.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BannerStatus {
    @SerialName("cancelled")
    CANCELLED,

    @SerialName("finished")
    FINISHED,
}
