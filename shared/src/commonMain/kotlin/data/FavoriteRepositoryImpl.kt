package data

import domain.event.Event
import domain.favorite.FavoriteRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post

/**
 * Ktor-based [FavoriteRepository], calling `POST /events/{id}/favorite` / `GET /profile/favorites`
 * (FAVUI-01/02) through the same authenticated [httpClient] used by other authenticated repos.
 */
class FavoriteRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String = ApiConfig.BaseUrl,
) : FavoriteRepository {

    override suspend fun toggle(eventId: String): Boolean {
        val response = httpClient.post("$baseUrl${ApiConfig.ApiV1Prefix}/events/$eventId/favorite")
        return response.body<FavoriteToggleResponseDto>().data.toDomain().favorited
    }

    override suspend fun list(): List<Event> {
        // SPEC_DEVIATION: tasks.md's T9 describes the endpoint as `GET /favorites`, but the
        // actual, only routed endpoint (routes/api_v1.php) is `GET /profile/favorites`.
        val response = httpClient.get("$baseUrl${ApiConfig.ApiV1Prefix}/profile/favorites")
        return response.body<EventListResponseDto>().toDomain().events
    }
}
