package domain.enum

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `qor-api`'s backed `City` enum (ARCHITECTURE §14.1) — the fixed 4-city Greater
 * Vitória region set (`PROJECT.md`). Backs `UserAddress.city`, `Venue.city`, `Event.city`.
 * See [EventStatus] for the no-catch-all/fail-loudly rule this file also follows.
 */
@Serializable
enum class City {
    @SerialName("vitoria")
    Vitoria,

    @SerialName("vila_velha")
    VilaVelha,

    @SerialName("serra")
    Serra,

    @SerialName("cariacica")
    Cariacica,
}
