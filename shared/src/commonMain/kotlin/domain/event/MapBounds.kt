package domain.event

/** Bounding-box query params for `GET /events/map` (MAPUI-01/03), matching `qor-api`'s `MapBounds`. */
data class MapBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
)
