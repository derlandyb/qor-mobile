package domain.event.usecase

import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import domain.event.MapBounds

class FakeEventRepository(
    private val page: EventPage = EventPage(emptyList(), null),
    private val detail: EventDetail? = null,
    private val mapEvents: List<Event> = emptyList(),
) : EventRepository {
    var lastCity: City? = null
    var lastGenre: String? = null
    var lastCursor: String? = null
    var lastEventId: String? = null
    var lastMapCity: City? = null
    var lastMapBounds: MapBounds? = null

    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage {
        lastCity = city
        lastGenre = genre
        lastCursor = cursor
        return page
    }

    override suspend fun findById(id: String): EventDetail {
        lastEventId = id
        return detail ?: error("no detail configured for $id")
    }

    override suspend fun getMapEvents(city: City?, bounds: MapBounds?): List<Event> {
        lastMapCity = city
        lastMapBounds = bounds
        return mapEvents
    }
}
