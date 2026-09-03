package domain.event

import domain.enum.City

/** Zero-framework-dependency repository port for the public event-discovery surface. */
interface EventRepository {
    suspend fun findUpcoming(city: City? = null, genre: String? = null, cursor: String? = null): EventPage
    suspend fun findById(id: String): EventDetail
}
