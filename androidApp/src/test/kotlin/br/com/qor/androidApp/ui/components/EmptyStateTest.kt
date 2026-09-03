package br.com.qor.androidApp.ui.components

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
class EmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN the default message WHEN EmptyState is rendered THEN it shows the pt-BR no-events copy`() {
        composeTestRule.setContent {
            EmptyState()
        }

        composeTestRule.onNodeWithText("Nenhum evento encontrado").assertExists()
    }

    @Test
    fun `GIVEN a custom message WHEN EmptyState is rendered THEN it shows that message instead`() {
        composeTestRule.setContent {
            EmptyState(message = "Nenhum favorito ainda")
        }

        composeTestRule.onNodeWithText("Nenhum favorito ainda").assertExists()
    }
}
