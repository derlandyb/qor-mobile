package com.qualorock.shared.detail

import com.qualorock.shared.domain.Venue
import com.qualorock.shared.domain.VerificationStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class LocationSectionVariantTest {
    private fun venue(
        staticMapUrl: String? = null,
        address: String? = null,
    ) = Venue(
        id = "v1",
        name = "Matrix",
        city = "Vitória",
        address = address,
        staticMapUrl = staticMapUrl,
        verificationStatus = VerificationStatus.VERIFIED,
    )

    @Test
    fun `given a venue with coordinates when resolving the location variant then the map variant is returned`() {
        val variant = LocationSectionVariant.from(venue(staticMapUrl = "https://maps.googleapis.com/x", address = "Rua Rio Branco, 100"))

        assertEquals(LocationSectionVariant.Map("https://maps.googleapis.com/x", "Rua Rio Branco, 100"), variant)
    }

    @Test
    fun `given a venue with only an address when resolving the location variant then the address-only variant is returned`() {
        val variant = LocationSectionVariant.from(venue(address = "Rua Rio Branco, 100"))

        assertEquals(LocationSectionVariant.AddressOnly("Rua Rio Branco, 100"), variant)
    }

    @Test
    fun `given a venue with neither coordinates nor address when resolving the location variant then the omitted variant is returned`() {
        val variant = LocationSectionVariant.from(venue())

        assertEquals(LocationSectionVariant.Omitted, variant)
    }
}
