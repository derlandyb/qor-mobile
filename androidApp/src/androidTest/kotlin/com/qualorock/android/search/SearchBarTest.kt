package com.qualorock.android.search

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class SearchBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_an_empty_query_when_the_search_bar_renders_then_the_placeholder_is_shown_and_no_clear_button() {
        composeTestRule.setContent {
            SearchBar(query = "", onQueryChange = {}, onClear = {})
        }

        composeTestRule.onNodeWithText("O que você quer ouvir?").assertExists()
        composeTestRule.onNodeWithContentDescription("Limpar busca").assertDoesNotExist()
    }

    @Test
    fun given_typed_text_when_the_search_bar_is_typed_into_then_onQueryChange_is_invoked() {
        var latestQuery: String? = null

        composeTestRule.setContent {
            SearchBar(query = "", onQueryChange = { latestQuery = it }, onClear = {})
        }

        composeTestRule.onNodeWithText("O que você quer ouvir?").performTextInput("forro")

        assert(latestQuery == "forro")
    }

    @Test
    fun given_a_non_empty_query_when_the_clear_button_is_tapped_then_onClear_is_invoked() {
        var cleared = false

        composeTestRule.setContent {
            SearchBar(query = "forro", onQueryChange = {}, onClear = { cleared = true })
        }

        composeTestRule.onNodeWithContentDescription("Limpar busca").performClick()

        assert(cleared)
    }
}
