package br.com.qor.androidApp.ui.components

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FormFieldsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN no error WHEN QorTextField is rendered THEN no error text is shown`() {
        composeTestRule.setContent {
            QorTextField(value = "", onValueChange = {}, label = "Nome", errorMessage = null)
        }

        composeTestRule.onNodeWithText("Nome").assertExists()
    }

    @Test
    fun `GIVEN an error message WHEN QorTextField is rendered THEN the pt-BR error is shown below it`() {
        composeTestRule.setContent {
            QorTextField(value = "", onValueChange = {}, label = "E-mail", errorMessage = "E-mail inválido.")
        }

        composeTestRule.onNodeWithText("E-mail inválido.").assertExists()
    }

    @Test
    fun `GIVEN a EmailField WHEN text is typed THEN onValueChange receives it`() {
        var value = ""
        composeTestRule.setContent {
            EmailField(value = value, onValueChange = { value = it }, errorMessage = null)
        }

        composeTestRule.onNodeWithText("E-mail").performTextInput("ana@example.com")

        assert(value == "ana@example.com")
    }

    @Test
    fun `GIVEN a PasswordField WHEN rendered THEN the reveal toggle is present`() {
        composeTestRule.setContent {
            PasswordField(value = "senha123", onValueChange = {}, errorMessage = null)
        }

        composeTestRule.onNodeWithText("Mostrar senha").assertExists()
    }
}
