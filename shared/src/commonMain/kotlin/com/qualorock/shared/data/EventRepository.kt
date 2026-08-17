package com.qualorock.shared.data

import com.qualorock.shared.domain.Event

interface EventRepository {
    suspend fun getEventFeed(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<EventPage>

    suspend fun getEventDetail(id: String): Result<Event>
}
