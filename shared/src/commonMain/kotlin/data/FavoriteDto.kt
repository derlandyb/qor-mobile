package data

import domain.favorite.Favorite
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FavoriteToggleResponseDto(
    val data: FavoriteToggleDto,
)

@Serializable
internal data class FavoriteToggleDto(
    @SerialName("event_id") val eventId: Int,
    val favorited: Boolean,
)

internal fun FavoriteToggleDto.toDomain(): Favorite = Favorite(
    eventId = eventId.toString(),
    favorited = favorited,
)
