package com.qualorock.shared.data

import com.qualorock.shared.domain.Event
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
    ): Result<EventPage> =
        runCatching {
            val response =
                httpClient.get("$baseUrl/api/events") {
                    parameter("limit", limit)
                    cursor?.let { parameter("cursor", it) }
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
