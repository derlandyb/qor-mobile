package com.qualorock.shared.detail

import com.qualorock.shared.domain.Venue

/** DETAIL-004's three-way rule as a pure, testable decision — static map, address-only, or omitted entirely. */
sealed interface LocationSectionVariant {
    data class Map(val url: String, val address: String?) : LocationSectionVariant

    data class AddressOnly(val address: String) : LocationSectionVariant

    data object Omitted : LocationSectionVariant

    companion object {
        fun from(venue: Venue): LocationSectionVariant =
            when {
                venue.staticMapUrl != null -> Map(venue.staticMapUrl, venue.address)
                venue.address != null -> AddressOnly(venue.address)
                else -> Omitted
            }
    }
}
