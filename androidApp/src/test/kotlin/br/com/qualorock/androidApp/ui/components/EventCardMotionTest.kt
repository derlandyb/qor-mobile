package br.com.qualorock.androidApp.ui.components

import org.junit.Test
import kotlin.test.assertEquals

class EventCardMotionTest {

    @Test
    fun `GIVEN not pressed WHEN pressScale is called THEN it is the resting 1x scale`() {
        assertEquals(1f, EventCardMotion.pressScale(pressed = false))
    }

    @Test
    fun `GIVEN pressed WHEN pressScale is called THEN it scales to 1_03x per design-system's hover spec`() {
        assertEquals(1.03f, EventCardMotion.pressScale(pressed = true))
    }

    @Test
    fun `GIVEN pressed WHEN pressRiseDp is called THEN it rises by 4dp`() {
        assertEquals(-4f, EventCardMotion.pressRiseDp(pressed = true))
    }

    @Test
    fun `GIVEN not pressed WHEN pressRiseDp is called THEN there is no rise`() {
        assertEquals(0f, EventCardMotion.pressRiseDp(pressed = false))
    }
}
