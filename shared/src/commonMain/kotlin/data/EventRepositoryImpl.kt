package data

import domain.enum.City
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/** Ktor-based [EventRepository], calling `api.md` T25's public `/api/v1/events` endpoints. */
class EventRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String = ApiConfig.BaseUrl,
) : EventRepository {

    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage {
        val response = httpClient.get("$baseUrl${ApiConfig.ApiV1Prefix}/events") {
            city?.let { parameter("city", it.wireValue()) }
            genre?.let { parameter("genre", it) }
            cursor?.let { parameter("cursor", it) }
        }
        return response.body<EventListResponseDto>().toDomain()
    }

    override suspend fun findById(id: String): EventDetail {
        val response = httpClient.get("$baseUrl${ApiConfig.ApiV1Prefix}/events/$id")
        return response.body<EventDetailResponseDto>().data.toEventDetail()
    }
}

/** The API's raw snake_case wire value for a [City] case, sourced from its `@SerialName`. */
internal fun City.wireValue(): String =
    (Json.encodeToJsonElement(City.serializer(), this) as JsonPrimitive).content
