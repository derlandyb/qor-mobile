package com.qualorock.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class Promoter(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val socialLinks: Map<String, String>? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val verificationStatus: VerificationStatus,
)
