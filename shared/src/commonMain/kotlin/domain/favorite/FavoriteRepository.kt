package domain.favorite

import domain.event.Event

/** Zero-framework-dependency repository port for favoriting/listing favorited events (FAVUI-01/02). */
interface FavoriteRepository {
    /** Calls `POST /events/{id}/favorite`, returning the new favorited state. */
    suspend fun toggle(eventId: String): Boolean

    /** Calls `GET /profile/favorites`, mapped to the same [Event] shape used elsewhere. */
    suspend fun list(): List<Event>
}
