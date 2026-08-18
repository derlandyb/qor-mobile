package com.qualorock.shared.map

import com.qualorock.shared.domain.Event
import com.qualorock.shared.domain.EventStatus
import com.qualorock.shared.domain.Venue
import com.qualorock.shared.domain.VerificationStatus
import com.qualorock.shared.filters.FilterState
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private val grandeVitoriaBounds = MapBounds(minLat = -20.6, maxLat = -20.1, minLng = -40.5, maxLng = -40.1)

private fun eventAt(
    id: String,
    lat: Double?,
    lng: Double?,
) = Event(
    id = id,
    title = "Show $id",
    startDateTime = Instant.parse("2026-08-16T22:00:00Z"),
    venue =
        Venue(
            id = "v-$id",
            name = "Venue $id",
            city = "Vitória",
            latitude = lat,
            longitude = lng,
            verificationStatus = VerificationStatus.VERIFIED,
        ),
    city = "Vitória",
    status = EventStatus.PUBLISHED,
)

class MapDisplayStateTest {
    @Test
    fun `given non-empty markers inside the visible bounds when derived then Markers is returned`() {
        val markers = listOf(eventAt("1", -20.31, -40.31))

        val state = MapDisplayStateDeriver.derive(markers, grandeVitoriaBounds, FilterState())

        assertIs<MapDisplayState.Markers>(state)
        assertEquals(listOf("1"), state.events.map { it.id })
    }

    @Test
    fun `given non-empty markers all outside the visible bounds when derived then EmptyViewport is returned`() {
        val markers = listOf(eventAt("1", -21.0, -41.0))

        val state = MapDisplayStateDeriver.derive(markers, grandeVitoriaBounds, FilterState())

        assertIs<MapDisplayState.EmptyViewport>(state)
    }

    @Test
    fun `given empty markers and active filters when derived then NoFilterResults with canClear is returned`() {
        val state = MapDisplayStateDeriver.derive(emptyList(), grandeVitoriaBounds, FilterState(city = "Vila Velha"))

        assertIs<MapDisplayState.NoFilterResults>(state)
        assertEquals(true, state.canClear)
    }

    @Test
    fun `given empty markers and no active filters when derived then EmptyViewport is returned`() {
        val state = MapDisplayStateDeriver.derive(emptyList(), grandeVitoriaBounds, FilterState())

        assertIs<MapDisplayState.EmptyViewport>(state)
    }
}
