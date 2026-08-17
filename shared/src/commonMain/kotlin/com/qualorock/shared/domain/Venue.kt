package com.qualorock.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class Venue(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val city: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val staticMapUrl: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val socialLinks: Map<String, String>? = null,
    val verificationStatus: VerificationStatus,
)
