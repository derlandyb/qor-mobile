package br.com.qualorock.androidApp.ui.components

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class GenreTagRenderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN a genre WHEN GenreTag is rendered THEN its uppercase label is displayed`() {
        composeTestRule.setContent {
            GenreTag(genre = "Rock")
        }

        composeTestRule.onNodeWithText("ROCK").assertExists()
    }
}
