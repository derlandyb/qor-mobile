package com.qualorock.shared.map

import com.qualorock.shared.data.createHttpClient
import com.qualorock.shared.domain.Event
import com.qualorock.shared.filters.DateBucket
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
private data class MapMarkersResponse(val data: List<Event>)

interface MapRepository {
    suspend fun getMapMarkers(
        dateBucket: DateBucket? = null,
        city: String? = null,
        genres: List<String> = emptyList(),
        artistId: String? = null,
    ): Result<List<Event>>
}

/**
 * Hits the unpaginated `/api/events/map` endpoint — unlike [com.qualorock.shared.data.EventRepository.getEventFeed],
 * it takes no `q`/`cursor` (free-text search and pagination are deliberately excluded from the map, per MAP design)
 * and returns the entire filtered, coordinate-valid set in one call.
 */
class KtorMapRepository(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient(),
) : MapRepository {
    override suspend fun getMapMarkers(
        dateBucket: DateBucket?,
        city: String?,
        genres: List<String>,
        artistId: String?,
    ): Result<List<Event>> =
        runCatching {
            httpClient
                .get("$baseUrl/api/events/map") {
                    dateBucket?.let { parameter("date_bucket", it.wireValue) }
                    city?.let { parameter("city", it) }
                    genres.forEach { parameter("genres[]", it) }
                    artistId?.let { parameter("artist_id", it) }
                }.body<MapMarkersResponse>()
                .data
        }

    /** Releases the underlying [HttpClient]'s connection resources — call when this repository's owner is discarded. */
    fun close() = httpClient.close()
}
