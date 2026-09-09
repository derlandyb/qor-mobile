package domain.favorite.usecase

import domain.favorite.FavoriteRepository

/**
 * Thin wrapper over [FavoriteRepository.toggle] (FAVUI-01), mirroring the existing
 * `GetEventDetails`/`ListUpcomingEvents` use-case shape. Wraps the repository's throwing
 * call in a [Result] so callers get a favorite-toggle-failure branch instead of an
 * uncaught exception (design.md's Error Handling Strategy).
 */
class ToggleFavorite(private val favoriteRepository: FavoriteRepository) {
    suspend fun execute(eventId: String): Result<Boolean> = runCatching { favoriteRepository.toggle(eventId) }
}
