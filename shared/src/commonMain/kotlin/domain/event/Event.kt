package domain.event

import domain.enum.City

/**
 * Mirrors `qor-api`'s `Event` entity shape (ARCHITECTURE §4) — the subset of fields the
 * public event-discovery surfaces need. `genre` is a plain [String] lookup value, not a Kotlin
 * enum, mirroring `qor-api`'s deliberate `Genre`-is-a-DB-lookup-table decision (ARCHITECTURE
 * §14.1) — modeling it as a compiled enum would mean a client release every time ops adds one.
 */
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val coverImageUrl: String?,
    val startsAt: String,
    val city: City,
    val genre: String,
    val address: String,
    val isFree: Boolean,
    val ticketUrl: String?,
)

/** One cursor-paginated page of [Event]s, per `api.md` T25's cursor-pagination contract. */
data class EventPage(
    val events: List<Event>,
    val nextCursor: String?,
)
