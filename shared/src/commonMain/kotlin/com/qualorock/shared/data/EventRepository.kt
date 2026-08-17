package com.qualorock.shared.data

import com.qualorock.shared.domain.Event
import com.qualorock.shared.filters.DateBucket

interface EventRepository {
    suspend fun getEventFeed(
        cursor: String? = null,
        limit: Int = 20,
        q: String? = null,
        dateBucket: DateBucket? = null,
        city: String? = null,
        genres: List<String> = emptyList(),
        artistId: String? = null,
    ): Result<EventPage>

    suspend fun getEventDetail(id: String): Result<Event>
}
