package com.qualorock.shared.data

import com.qualorock.shared.domain.Event
import kotlinx.serialization.Serializable

@Serializable
internal data class EventDetailEnvelope(
    val data: Event,
)

class EventNotFoundException(val eventId: String) : Exception()
