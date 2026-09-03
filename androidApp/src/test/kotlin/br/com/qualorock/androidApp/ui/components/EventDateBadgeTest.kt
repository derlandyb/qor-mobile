package br.com.qualorock.androidApp.ui.components

import org.junit.Test
import kotlin.test.assertEquals

class EventDateBadgeTest {

    @Test
    fun `GIVEN a September ISO datetime WHEN formatDateBadge is called THEN it maps to the pt-BR month abbreviation and day`() {
        val label = formatDateBadge("2026-09-20T22:00:00Z")

        assertEquals("SET", label.month)
        assertEquals("20", label.day)
    }

    @Test
    fun `GIVEN a January ISO datetime WHEN formatDateBadge is called THEN it maps to JAN`() {
        assertEquals("JAN", formatDateBadge("2026-01-05T20:00:00Z").month)
    }

    @Test
    fun `GIVEN an ISO datetime WHEN formatEventTime is called THEN it extracts the hour and minute portion`() {
        assertEquals("22:00", formatEventTime("2026-09-20T22:00:00Z"))
    }
}
