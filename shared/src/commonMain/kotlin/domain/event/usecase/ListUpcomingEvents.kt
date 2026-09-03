package domain.event.usecase

import domain.enum.City
import domain.event.EventPage
import domain.event.EventRepository

/** Thin wrapper over [EventRepository.findUpcoming], per `api.md` T23's client-facing contract. */
class ListUpcomingEvents(private val eventRepository: EventRepository) {
    suspend fun execute(city: City? = null, genre: String? = null, cursor: String? = null): EventPage =
        eventRepository.findUpcoming(city, genre, cursor)
}
