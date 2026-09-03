package br.com.qualorock.androidApp.ui.components

import org.junit.Test
import kotlin.test.assertEquals

class CtaButtonMotionTest {

    @Test
    fun `GIVEN not pressed WHEN mapaBackgroundAlpha is called THEN it is the resting 10pct tint`() {
        assertEquals(CtaButtonMotion.MapaRestingAlpha, CtaButtonMotion.mapaBackgroundAlpha(pressed = false))
    }

    @Test
    fun `GIVEN pressed WHEN mapaBackgroundAlpha is called THEN it is fully solid`() {
        assertEquals(1f, CtaButtonMotion.mapaBackgroundAlpha(pressed = true))
    }

    @Test
    fun `GIVEN not pressed WHEN instagramGradientOffset is called THEN the gradient sits at its left rest position`() {
        assertEquals(0f, CtaButtonMotion.instagramGradientOffset(pressed = false))
    }

    @Test
    fun `GIVEN pressed WHEN instagramGradientOffset is called THEN the gradient shifts fully right`() {
        assertEquals(1f, CtaButtonMotion.instagramGradientOffset(pressed = true))
    }
}
