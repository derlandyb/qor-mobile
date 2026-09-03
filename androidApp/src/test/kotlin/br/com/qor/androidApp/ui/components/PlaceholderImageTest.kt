package br.com.qor.androidApp.ui.components

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlaceholderImageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN it is rendered THEN it exposes a generic flyer content description`() {
        composeTestRule.setContent {
            PlaceholderImage()
        }

        composeTestRule.onNodeWithContentDescription("Sem imagem de divulgação").assertExists()
    }
}
