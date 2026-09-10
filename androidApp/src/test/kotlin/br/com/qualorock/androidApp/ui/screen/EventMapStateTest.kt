package br.com.qualorock.androidApp.ui.screen

import domain.enum.City
import domain.event.Event
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A22 — pure unit tests for the geocoding-result-to-map-state mapping (no Robolectric needed). */
class EventMapStateTest {

    @Test
    fun `GIVEN a non-empty geocode result WHEN mapped THEN Located wraps the first point`() {
        val first = GeoPoint(latitude = -20.3155, longitude = -40.3128)
        val second = GeoPoint(latitude = -20.0, longitude = -40.0)

        val state = toEventMapState(listOf(first, second))

        assertEquals(EventMapState.Located(first), state)
    }

    @Test
    fun `GIVEN an empty geocode result WHEN mapped THEN Failed is returned`() {
        val state = toEventMapState(emptyList())

        assertEquals(EventMapState.Failed, state)
    }

    @Test
    fun `GIVEN a null geocode result WHEN mapped THEN Failed is returned`() {
        val state = toEventMapState(null)

        assertEquals(EventMapState.Failed, state)
    }
}

private fun sampleEvent(id: String, latitude: Double?, longitude: Double?) = Event(
    id = id,
    title = "Show $id",
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = City.Vitoria,
    genre = "Rock",
    address = "Rua X, 100",
    isFree = true,
    ticketUrl = null,
    latitude = latitude,
    longitude = longitude,
)

/** T32 (MAPUI-01/04) — pure unit tests for [toMapPins], `MapScreen`'s server-geocoded multi-pin mapping. */
class ToMapPinsTest {

    @Test
    fun `GIVEN events with resolved coordinates WHEN mapped THEN a pin is produced per event`() {
        val geocoded = sampleEvent("e1", latitude = -20.3155, longitude = -40.3128)

        val pins = toMapPins(listOf(geocoded))

        assertEquals(1, pins.size)
        assertEquals(geocoded, pins.first().event)
        assertEquals(GeoPoint(-20.3155, -40.3128), pins.first().point)
    }

    @Test
    fun `GIVEN an event with a null coordinate WHEN mapped THEN it is excluded, not crashed on`() {
        val geocoded = sampleEvent("e1", latitude = -20.3155, longitude = -40.3128)
        val ungeocoded = sampleEvent("e2", latitude = null, longitude = null)

        val pins = toMapPins(listOf(geocoded, ungeocoded))

        assertEquals(1, pins.size)
        assertEquals("e1", pins.first().event.id)
    }

    @Test
    fun `GIVEN no events WHEN mapped THEN an empty pin list is returned, a valid state`() {
        val pins = toMapPins(emptyList())

        assertTrue(pins.isEmpty())
    }
}
