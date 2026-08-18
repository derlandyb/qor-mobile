package com.qualorock.android.feed

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.qualorock.shared.domain.Event
import com.qualorock.shared.domain.EventStatus
import com.qualorock.shared.domain.Venue
import com.qualorock.shared.domain.VerificationStatus
import com.qualorock.shared.feed.DateGroup
import com.qualorock.shared.feed.EventFeedUiState
import com.qualorock.shared.filters.FeedResultsUiState
import com.qualorock.shared.filters.FilterState
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class EventFeedScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun event(id: String) =
        Event(
            id = id,
            title = "Show $id",
            startDateTime = Instant.parse("2026-08-16T22:00:00Z"),
            venue = Venue(id = "v-$id", name = "Venue $id", city = "Vitória", verificationStatus = VerificationStatus.VERIFIED),
            city = "Vitória",
            status = EventStatus.PUBLISHED,
        )

    @Test
    fun `given_an_anonymous_visitor_when_the_feed_loads_then_upcoming_events_are_grouped_by_date`() {
        // Distinct from FilterBar's own "Hoje" date-preset chip, which now renders on every EventFeedScreen state.
        val state =
            EventFeedUiState(
                groupedEvents = listOf(DateGroup(label = "15 de agosto", events = listOf(event("1")))),
                endReached = true,
            )

        composeTestRule.setContent {
            EventFeedScreen(
                state = state,
                onLoadNextPage = {},
                onRetry = {},
                onEventClick = {},
                onFavoriteClick = {},
                onShareClick = {},
            )
        }

        composeTestRule.onNodeWithText("15 de agosto").assertExists()
        composeTestRule.onNodeWithText("Show 1").assertExists()
    }

    @Test
    fun `given_zero_events_when_the_feed_loads_then_the_empty_state_is_reachable`() {
        composeTestRule.setContent {
            EventFeedScreen(
                state = EventFeedUiState(),
                onLoadNextPage = {},
                onRetry = {},
                onEventClick = {},
                onFavoriteClick = {},
                onShareClick = {},
            )
        }

        composeTestRule.onNodeWithText("Nenhum show por aqui ainda").assertExists()
    }

    @Test
    fun `given_an_initial_load_failure_when_the_feed_loads_then_the_retry_state_is_reachable`() {
        var retried = false

        composeTestRule.setContent {
            EventFeedScreen(
                state = EventFeedUiState(error = com.qualorock.shared.feed.FeedError.INITIAL_LOAD),
                onLoadNextPage = {},
                onRetry = { retried = true },
                onEventClick = {},
                onFavoriteClick = {},
                onShareClick = {},
            )
        }

        composeTestRule.onNodeWithText("Tentar novamente").performClick()
        assert(retried)
    }

    @Test
    fun given_a_results_state_when_the_feed_renders_then_the_flat_results_list_replaces_the_grouped_feed() {
        val groupedState =
            EventFeedUiState(groupedEvents = listOf(DateGroup(label = "15 de agosto", events = listOf(event("1")))))
        val resultsState = FeedResultsUiState.Results(events = listOf(event("2")), activeFilters = FilterState(), q = "forro")

        composeTestRule.setContent {
            EventFeedScreen(
                state = groupedState,
                onLoadNextPage = {},
                onRetry = {},
                onEventClick = {},
                onFavoriteClick = {},
                onShareClick = {},
                resultsState = resultsState,
            )
        }

        composeTestRule.onNodeWithText("Show 2").assertExists()
        composeTestRule.onNodeWithText("Show 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("15 de agosto").assertDoesNotExist()
    }

    @Test
    fun given_a_no_results_state_when_the_feed_renders_then_the_no_results_message_and_clear_filters_action_are_shown() {
        var cleared = false
        val resultsState =
            FeedResultsUiState.NoResults(
                activeFilters = FilterState(city = "Vila Velha"),
                q = "forro",
            )

        composeTestRule.setContent {
            EventFeedScreen(
                state = EventFeedUiState(),
                onLoadNextPage = {},
                onRetry = {},
                onEventClick = {},
                onFavoriteClick = {},
                onShareClick = {},
                resultsState = resultsState,
                onClearAllFilters = { cleared = true },
            )
        }

        composeTestRule.onNodeWithText("Nenhum evento encontrado com esses filtros").assertExists()
        composeTestRule.onNodeWithText("\"forro\" · Vila Velha").assertExists()
        composeTestRule.onNodeWithText("Limpar filtros").performClick()
        assert(cleared)
    }
}
