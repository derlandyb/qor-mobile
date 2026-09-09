package domain.favorite

/**
 * Result of toggling an event's favorited state via `POST /events/{id}/favorite`
 * (FAVUI-01). The API returns both fields together, so the domain models them together
 * rather than [FavoriteRepository.toggle] handing back a bare, context-free [Boolean].
 */
data class Favorite(
    val eventId: String,
    val favorited: Boolean,
)
