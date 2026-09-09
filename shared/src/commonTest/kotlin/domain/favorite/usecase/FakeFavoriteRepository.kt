package domain.favorite.usecase

import domain.event.Event
import domain.favorite.FavoriteRepository

class FakeFavoriteRepository(
    private val toggleResult: Boolean? = null,
    private val toggleError: Throwable? = null,
    private val listResult: List<Event> = emptyList(),
    private val listError: Throwable? = null,
) : FavoriteRepository {
    var lastToggledEventId: String? = null

    override suspend fun toggle(eventId: String): Boolean {
        lastToggledEventId = eventId
        toggleError?.let { throw it }
        return toggleResult ?: error("no toggle result configured")
    }

    override suspend fun list(): List<Event> {
        listError?.let { throw it }
        return listResult
    }
}
