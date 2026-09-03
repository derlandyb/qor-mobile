package br.com.qor.androidApp.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import domain.enum.City
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CityFilterBarRenderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN Vitoria is selected WHEN the bar renders THEN only its pill is marked selected`() {
        composeTestRule.setContent {
            CityFilterBar(selected = City.Vitoria, onSelect = {})
        }

        composeTestRule.onNodeWithText("VITÓRIA").assertIsSelected()
        composeTestRule.onNodeWithText("SERRA").assertIsNotSelected()
    }

    @Test
    fun `GIVEN the bar is rendered WHEN a pill is clicked THEN onSelect is called with that city`() {
        var selected: City? = null
        composeTestRule.setContent {
            CityFilterBar(selected = City.Vitoria, onSelect = { selected = it })
        }

        composeTestRule.onNodeWithText("SERRA").performClick()

        assert(selected == City.Serra)
    }
}
