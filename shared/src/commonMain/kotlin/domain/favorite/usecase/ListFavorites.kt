package domain.favorite.usecase

import domain.event.Event
import domain.favorite.FavoriteRepository

/**
 * Thin wrapper over [FavoriteRepository.list] (FAVUI-02), mirroring the existing
 * `GetEventDetails`/`ListUpcomingEvents` use-case shape.
 */
class ListFavorites(private val favoriteRepository: FavoriteRepository) {
    suspend fun execute(): Result<List<Event>> = runCatching { favoriteRepository.list() }
}
