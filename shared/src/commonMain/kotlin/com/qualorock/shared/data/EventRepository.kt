package com.qualorock.shared.data

interface EventRepository {
    suspend fun getEventFeed(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<EventPage>
}
