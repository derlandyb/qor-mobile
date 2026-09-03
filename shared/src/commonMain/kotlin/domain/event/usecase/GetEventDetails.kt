package domain.event.usecase

import domain.event.EventDetail
import domain.event.EventRepository

/** Thin wrapper over [EventRepository.findById], per `api.md` T24's client-facing contract. */
class GetEventDetails(private val eventRepository: EventRepository) {
    suspend fun execute(eventId: String): EventDetail = eventRepository.findById(eventId)
}
