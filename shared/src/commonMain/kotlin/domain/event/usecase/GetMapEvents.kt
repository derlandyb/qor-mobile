package domain.event.usecase

import domain.enum.City
import domain.event.Event
import domain.event.EventRepository
import domain.event.MapBounds

/**
 * Thin wrapper over [EventRepository.getMapEvents] (MAPUI-01/03), mirroring the existing
 * `GetEventDetails`/`ListUpcomingEvents` use-case shape. Wraps the repository's throwing call
 * in a [Result] per design.md's Interfaces section for this slice.
 */
class GetMapEvents(private val eventRepository: EventRepository) {
    suspend fun execute(city: City? = null, bounds: MapBounds? = null): Result<List<Event>> =
        runCatching { eventRepository.getMapEvents(city, bounds) }
}
