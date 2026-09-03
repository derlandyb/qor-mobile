package br.com.qualorock.androidApp.ui.components

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LivePulseBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN the badge is rendered THEN it shows the pt-BR live label and an accessible description`() {
        composeTestRule.setContent {
            LivePulseBadge()
        }

        composeTestRule.onNodeWithText("Ao vivo").assertExists()
        composeTestRule.onNodeWithContentDescription("Ao vivo agora").assertExists()
    }
}
