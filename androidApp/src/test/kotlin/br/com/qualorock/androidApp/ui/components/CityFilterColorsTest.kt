package br.com.qualorock.androidApp.ui.components

import androidx.compose.ui.graphics.Color
import br.com.qualorock.androidApp.R
import design.NightlifeGvTokens
import domain.enum.City
import org.junit.Test
import kotlin.test.assertEquals

class CityFilterColorsTest {

    @Test
    fun `GIVEN Vitoria WHEN styleFor is called THEN it uses accent-pink and the pt-BR label resource`() {
        val style = CityFilterColors.styleFor(City.Vitoria)
        assertEquals(Color(NightlifeGvTokens.AccentPink), style.activeColor)
        assertEquals(R.string.city_vitoria, style.labelRes)
    }

    @Test
    fun `GIVEN VilaVelha WHEN styleFor is called THEN it uses accent-blue`() {
        assertEquals(Color(NightlifeGvTokens.AccentBlue), CityFilterColors.styleFor(City.VilaVelha).activeColor)
    }

    @Test
    fun `GIVEN Serra WHEN styleFor is called THEN it uses accent-orange`() {
        assertEquals(Color(NightlifeGvTokens.AccentOrange), CityFilterColors.styleFor(City.Serra).activeColor)
    }

    @Test
    fun `GIVEN Cariacica WHEN styleFor is called THEN it uses accent-purple with white active text`() {
        val style = CityFilterColors.styleFor(City.Cariacica)
        assertEquals(Color(NightlifeGvTokens.AccentPurple), style.activeColor)
        assertEquals(Color.White, style.activeTextColor)
    }
}
