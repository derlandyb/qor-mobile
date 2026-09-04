package br.com.qualorock.androidApp.ui.screen

import org.junit.Test
import kotlin.test.assertEquals

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
