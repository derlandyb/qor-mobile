package br.com.qualorock.androidApp.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
class BottomNavTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN Inicio is the current destination WHEN the bar renders THEN its item is marked selected`() {
        composeTestRule.setContent {
            BottomNav(current = BottomNavDestination.Inicio, onSelect = {})
        }

        composeTestRule.onNodeWithText("Início").assertIsSelected()
    }

    @Test
    fun `GIVEN the bar is rendered WHEN Explorar is clicked THEN onSelect is called with Explorar`() {
        var selected: BottomNavDestination? = null
        composeTestRule.setContent {
            BottomNav(current = BottomNavDestination.Inicio, onSelect = { selected = it })
        }

        composeTestRule.onNodeWithText("Explorar").performClick()

        assert(selected == BottomNavDestination.Explorar)
    }

    @Test
    fun `GIVEN Favoritos is not yet wired to a real screen THEN its item is disabled`() {
        composeTestRule.setContent {
            BottomNav(current = BottomNavDestination.Inicio, onSelect = {})
        }

        composeTestRule.onNodeWithText("Favoritos").assertIsNotEnabled()
    }
}
