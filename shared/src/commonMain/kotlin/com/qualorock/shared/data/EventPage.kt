package com.qualorock.shared.data

import com.qualorock.shared.domain.Event
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class EventPage(
    val events: List<Event>,
    val nextCursor: String?,
)

@Serializable
internal data class EventFeedResponse(
    val data: List<Event>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)
