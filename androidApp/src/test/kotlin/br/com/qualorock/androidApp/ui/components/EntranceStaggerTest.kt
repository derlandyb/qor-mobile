package br.com.qualorock.androidApp.ui.components

import design.NightlifeGvTokens
import org.junit.Test
import kotlin.test.assertEquals

class EntranceStaggerTest {

    @Test
    fun `GIVEN index 0 WHEN entranceStaggerDelayMillis is called THEN there is no delay`() {
        assertEquals(0L, entranceStaggerDelayMillis(0))
    }

    @Test
    fun `GIVEN index 3 WHEN entranceStaggerDelayMillis is called THEN the delay is 3 times duration-stagger`() {
        assertEquals(NightlifeGvTokens.DurationStaggerMs.toLong() * 3, entranceStaggerDelayMillis(3))
    }
}
