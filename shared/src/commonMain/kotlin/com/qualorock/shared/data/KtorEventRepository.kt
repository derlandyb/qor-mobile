package com.qualorock.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

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
}
