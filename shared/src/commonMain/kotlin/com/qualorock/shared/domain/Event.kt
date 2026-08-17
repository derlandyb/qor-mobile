package com.qualorock.shared.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    val title: String,
    val coverImageUrl: String? = null,
    @Serializable(with = InstantIso8601Serializer::class)
    val startDateTime: Instant,
    val venue: Venue,
    val city: String,
    val price: Price? = null,
    val ageRating: AgeRating? = null,
    val genres: List<String> = emptyList(),
    val ticketUrl: String? = null,
    val status: EventStatus,
    val isFavorited: Boolean? = null,
)
