package br.com.qor.androidApp.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ConsentCaptureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN it is rendered THEN the acceptance checkbox starts unchecked`() {
        composeTestRule.setContent {
            ConsentCapture(accepted = false, onAcceptedChange = {})
        }

        composeTestRule.onNodeWithTag(ConsentCheckboxTestTag).assertIsOff()
    }

    @Test
    fun `GIVEN it is rendered WHEN the checkbox is tapped THEN onAcceptedChange fires with true`() {
        var accepted: Boolean? = null
        composeTestRule.setContent {
            ConsentCapture(accepted = false, onAcceptedChange = { accepted = it })
        }

        composeTestRule.onNodeWithTag(ConsentCheckboxTestTag).performClick()

        assert(accepted == true)
    }
}
