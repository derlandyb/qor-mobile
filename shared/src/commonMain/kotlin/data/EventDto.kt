package data

import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventPromoterContact
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventListResponseDto(
    val data: List<EventDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
internal data class EventDetailResponseDto(
    val data: EventDto,
)

@Serializable
internal data class EventDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("starts_at") val startsAt: String,
    val city: City,
    val genre: String,
    val address: String,
    @SerialName("is_free") val isFree: Boolean,
    @SerialName("ticket_url") val ticketUrl: String? = null,
    val status: String,
    val promoters: List<EventPromoterDto> = emptyList(),
)

@Serializable
internal data class EventPromoterDto(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val instagram: String? = null,
    val tiktok: String? = null,
)

internal fun EventDto.toDomain(): Event = Event(
    id = id,
    title = title,
    description = description,
    coverImageUrl = coverImageUrl,
    startsAt = startsAt,
    city = city,
    genre = genre,
    address = address,
    isFree = isFree,
    ticketUrl = ticketUrl,
)

internal fun EventListResponseDto.toDomain(): EventPage = EventPage(
    events = data.map { it.toDomain() },
    nextCursor = nextCursor,
)

/**
 * Maps the raw `status` string to a distinct [EventDetail] variant so a cancelled/ended
 * payload can never be mistaken for [EventDetail.Active] downstream (S6's "Done when").
 */
internal fun EventDto.toEventDetail(): EventDetail {
    val promoterContacts = promoters.map {
        EventPromoterContact(
            name = it.name,
            phone = it.phone,
            email = it.email,
            instagram = it.instagram,
            tiktok = it.tiktok,
        )
    }
    val event = toDomain()
    return when (status) {
        "cancelled" -> EventDetail.Cancelled(event, promoterContacts)
        "ended" -> EventDetail.Ended(event, promoterContacts)
        else -> EventDetail.Active(event, promoterContacts)
    }
}
