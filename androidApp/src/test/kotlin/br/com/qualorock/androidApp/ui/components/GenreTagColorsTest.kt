package br.com.qualorock.androidApp.ui.components

import androidx.compose.ui.graphics.Color
import design.QualORockThemeTokens
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenreTagColorsTest {

    @Test
    fun `GIVEN Rock WHEN styleFor is called THEN it uses the accent-orange 15pct tint, not solid`() {
        val style = GenreTagColors.styleFor("Rock")
        assertEquals(Color(QualORockThemeTokens.AccentOrange), style.textColor)
        assertFalse(style.solidBackground)
    }

    @Test
    fun `GIVEN Sertanejo WHEN styleFor is called THEN it uses a solid accent-pink background`() {
        val style = GenreTagColors.styleFor("Sertanejo")
        assertTrue(style.solidBackground)
        assertEquals(Color(QualORockThemeTokens.AccentPink), style.backgroundColor)
    }

    @Test
    fun `GIVEN an unknown genre string WHEN styleFor is called THEN it falls back to a neutral tint`() {
        val style = GenreTagColors.styleFor("Forró")
        assertEquals(Color(QualORockThemeTokens.ColorTextSecondary), style.textColor)
        assertFalse(style.solidBackground)
    }

    @Test
    fun `GIVEN mixed case genre input WHEN styleFor is called THEN matching is case-insensitive`() {
        assertEquals(GenreTagColors.styleFor("reggae"), GenreTagColors.styleFor("REGGAE"))
    }
}
