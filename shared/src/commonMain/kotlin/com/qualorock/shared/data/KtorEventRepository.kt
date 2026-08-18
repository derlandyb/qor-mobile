package com.qualorock.shared.data

import com.qualorock.shared.domain.Event
import com.qualorock.shared.filters.DateBucket
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode

class KtorEventRepository(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient(),
) : EventRepository {
    override suspend fun getEventFeed(
        cursor: String?,
        limit: Int,
        q: String?,
        dateBucket: DateBucket?,
        city: String?,
        genres: List<String>,
        artistId: String?,
    ): Result<EventPage> =
        runCatching {
            val response =
                httpClient.get("$baseUrl/api/events") {
                    parameter("limit", limit)
                    cursor?.let { parameter("cursor", it) }
                    q?.let { parameter("q", it) }
                    dateBucket?.let { parameter("date_bucket", it.wireValue) }
                    city?.let { parameter("city", it) }
                    genres.forEach { parameter("genres[]", it) }
                    artistId?.let { parameter("artist_id", it) }
                }
            val body = response.body<EventFeedResponse>()
            EventPage(events = body.data, nextCursor = body.nextCursor)
        }

    override suspend fun getEventDetail(id: String): Result<Event> =
        runCatching {
            val response = httpClient.get("$baseUrl/api/events/$id")
            if (response.status == HttpStatusCode.NotFound) throw EventNotFoundException(id)
            response.body<EventDetailEnvelope>().data
        }

    /** Releases the underlying [HttpClient]'s connection resources — call when this repository's owner is discarded. */
    fun close() = httpClient.close()
}
