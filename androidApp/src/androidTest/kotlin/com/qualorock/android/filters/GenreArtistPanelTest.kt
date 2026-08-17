package com.qualorock.android.filters

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.qualorock.shared.filters.ArtistOption
import com.qualorock.shared.filters.OptionsUiState
import org.junit.Rule
import org.junit.Test

class GenreArtistPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_an_empty_genre_option_list_when_the_panel_renders_then_the_disabled_state_is_shown() {
        composeTestRule.setContent {
            GenreArtistPanel(
                genreOptions = OptionsUiState.Loaded(emptyList()),
                artistOptions = OptionsUiState.Loading,
                selectedGenres = emptySet(),
                selectedArtist = null,
                onToggleGenre = {},
                onSelectArtist = {},
            )
        }

        composeTestRule.onNodeWithText("Nada para filtrar ainda").assertExists()
    }

    @Test
    fun given_loaded_genre_options_when_a_genre_pill_is_tapped_then_onToggleGenre_is_invoked() {
        var toggled: String? = null

        composeTestRule.setContent {
            GenreArtistPanel(
                genreOptions = OptionsUiState.Loaded(listOf("Rock", "Samba")),
                artistOptions = OptionsUiState.Loaded(emptyList()),
                selectedGenres = emptySet(),
                selectedArtist = null,
                onToggleGenre = { toggled = it },
                onSelectArtist = {},
            )
        }

        composeTestRule.onNodeWithText("Rock").performClick()

        assert(toggled == "Rock")
    }

    @Test
    fun given_loaded_artist_options_when_an_artist_pill_is_tapped_then_onSelectArtist_is_invoked() {
        var selected: ArtistOption? = null
        val jorge = ArtistOption(id = "1", name = "Jorge & the Band")

        composeTestRule.setContent {
            GenreArtistPanel(
                genreOptions = OptionsUiState.Loaded(emptyList()),
                artistOptions = OptionsUiState.Loaded(listOf(jorge)),
                selectedGenres = emptySet(),
                selectedArtist = null,
                onToggleGenre = {},
                onSelectArtist = { selected = it },
            )
        }

        composeTestRule.onNodeWithText("Jorge & the Band").performClick()

        assert(selected == jorge)
    }
}
