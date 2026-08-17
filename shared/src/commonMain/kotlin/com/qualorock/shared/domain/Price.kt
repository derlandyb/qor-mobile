package com.qualorock.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class Price(
    val isFree: Boolean,
    val min: Double? = null,
    val max: Double? = null,
    val currency: String,
)
