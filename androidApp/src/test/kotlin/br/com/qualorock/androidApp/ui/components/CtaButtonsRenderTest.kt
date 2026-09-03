package br.com.qualorock.androidApp.ui.components

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CtaButtonsRenderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN MapaCta is rendered WHEN it is clicked THEN onClick fires`() {
        var clicked = false
        composeTestRule.setContent {
            MapaCta(onClick = { clicked = true })
        }

        composeTestRule.onNodeWithText("Ver no Mapa").performClick()

        assert(clicked)
    }

    @Test
    fun `GIVEN InstagramCta is rendered WHEN it is clicked THEN onClick fires`() {
        var clicked = false
        composeTestRule.setContent {
            InstagramCta(onClick = { clicked = true })
        }

        composeTestRule.onNodeWithText("Ver Instagram").performClick()

        assert(clicked)
    }

    @Test
    fun `GIVEN PrimaryButton is enabled WHEN it is clicked THEN onClick fires`() {
        var clicked = false
        composeTestRule.setContent {
            PrimaryButton(text = "Entrar", onClick = { clicked = true })
        }

        composeTestRule.onNodeWithText("Entrar").performClick()

        assert(clicked)
    }

    @Test
    fun `GIVEN PrimaryButton is loading WHEN rendered THEN the label is replaced by a progress indicator`() {
        composeTestRule.setContent {
            PrimaryButton(text = "Entrar", onClick = {}, isLoading = true)
        }

        composeTestRule.onAllNodesWithText("Entrar").assertCountEquals(0)
    }

    @Test
    fun `GIVEN PrimaryButton is disabled WHEN it is clicked THEN onClick does not fire`() {
        var clicked = false
        composeTestRule.setContent {
            PrimaryButton(text = "Entrar", onClick = { clicked = true }, enabled = false)
        }

        composeTestRule.onNodeWithText("Entrar").performClick()

        assert(!clicked)
    }

    @Test
    fun `GIVEN SecondaryButton is enabled WHEN it is clicked THEN onClick fires`() {
        var clicked = false
        composeTestRule.setContent {
            SecondaryButton(text = "Entrar com Google", onClick = { clicked = true })
        }

        composeTestRule.onNodeWithText("Entrar com Google").performClick()

        assert(clicked)
    }

    @Test
    fun `GIVEN SecondaryButton is disabled WHEN it is clicked THEN onClick does not fire`() {
        var clicked = false
        composeTestRule.setContent {
            SecondaryButton(text = "Entrar com Google", onClick = { clicked = true }, enabled = false)
        }

        composeTestRule.onNodeWithText("Entrar com Google").performClick()

        assert(!clicked)
    }
}
