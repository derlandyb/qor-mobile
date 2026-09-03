package domain.event

/** A tagged promoter's contact info, per `GetEventDetails`' "omit only the missing field" rule. */
data class EventPromoterContact(
    val name: String,
    val phone: String?,
    val email: String?,
    val instagram: String?,
    val tiktok: String?,
)

/**
 * Full event detail, per `api.md` T24's `GetEventDetails` contract. Modeled as a **sealed
 * class** so a cancelled/ended event is structurally distinct from a normal ([Active]) one —
 * client UI can never accidentally render a cancelled event's detail as if it were live, the
 * way a flat `status: EventStatus` field would allow (a missed `when` branch would still
 * compile).
 */
sealed class EventDetail {
    abstract val event: Event
    abstract val promoters: List<EventPromoterContact>

    data class Active(
        override val event: Event,
        override val promoters: List<EventPromoterContact> = emptyList(),
    ) : EventDetail()

    data class Cancelled(
        override val event: Event,
        override val promoters: List<EventPromoterContact> = emptyList(),
    ) : EventDetail()

    data class Ended(
        override val event: Event,
        override val promoters: List<EventPromoterContact> = emptyList(),
    ) : EventDetail()
}
