package domain.event

import domain.enum.City

/** Zero-framework-dependency repository port for the public event-discovery surface. */
interface EventRepository {
    suspend fun findUpcoming(city: City? = null, genre: String? = null, cursor: String? = null): EventPage
    suspend fun findById(id: String): EventDetail

    /** Calls `GET /events/map` (MAPUI-01/03) with either a bounding box or a city, not both required. */
    suspend fun getMapEvents(city: City? = null, bounds: MapBounds? = null): List<Event>
}
