package com.qualorock.android.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.qualorock.shared.domain.Event
import com.qualorock.shared.domain.EventStatus
import com.qualorock.shared.domain.Venue
import com.qualorock.shared.domain.VerificationStatus
import com.qualorock.shared.map.MapDisplayState
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class MapScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun event(id: String) =
        Event(
            id = id,
            title = "Show $id",
            startDateTime = Instant.parse("2026-08-16T22:00:00Z"),
            venue =
                Venue(
                    id = "v-$id",
                    name = "Venue $id",
                    city = "Vitória",
                    latitude = -20.31,
                    longitude = -40.31,
                    verificationStatus = VerificationStatus.VERIFIED,
                ),
            city = "Vitória",
            status = EventStatus.PUBLISHED,
        )

    @Test
    fun given_a_visitor_when_selecting_a_marker_then_its_event_preview_opens() {
        var opened: Event? = null
        composeTestRule.setContent {
            MapSheetsHost(
                previewEvent = event("1"),
                clusterEvents = null,
                onDismissPreview = {},
                onOpenDetail = { opened = it },
                onDismissList = {},
                onSelectFromList = {},
            )
        }

        composeTestRule.onNodeWithTag("marker_preview_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_detail_button").performClick()

        assert(opened?.id == "1")
    }

    @Test
    fun given_a_visitor_when_selecting_a_cluster_marker_then_every_constituent_event_is_listed() {
        composeTestRule.setContent {
            MapSheetsHost(
                previewEvent = null,
                clusterEvents = listOf(event("1"), event("2")),
                onDismissPreview = {},
                onOpenDetail = {},
                onDismissList = {},
                onSelectFromList = {},
            )
        }

        composeTestRule.onNodeWithTag("multi_event_list_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show 2").assertIsDisplayed()
    }

    @Test
    fun given_a_multi_event_list_when_an_item_is_tapped_then_its_own_preview_is_selected() {
        var selected: Event? = null
        composeTestRule.setContent {
            MapSheetsHost(
                previewEvent = null,
                clusterEvents = listOf(event("1"), event("2")),
                onDismissPreview = {},
                onOpenDetail = {},
                onDismissList = {},
                onSelectFromList = { selected = it },
            )
        }

        composeTestRule.onNodeWithText("Show 2").performClick()

        assert(selected?.id == "2")
    }

    @Test
    fun given_zero_markers_and_no_active_filters_when_rendered_then_the_empty_viewport_message_is_shown() {
        composeTestRule.setContent {
            MapDisplayStateBanner(displayState = MapDisplayState.EmptyViewport, onClearFilters = {})
        }

        composeTestRule.onNodeWithTag("empty_viewport_state").assertIsDisplayed()
    }

    @Test
    fun given_zero_markers_and_active_filters_when_rendered_then_no_filter_results_offers_clear_filters() {
        var cleared = false
        composeTestRule.setContent {
            MapDisplayStateBanner(
                displayState = MapDisplayState.NoFilterResults(canClear = true),
                onClearFilters = { cleared = true },
            )
        }

        composeTestRule.onNodeWithTag("no_filter_results_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clear_filters_button").performClick()

        assert(cleared)
    }

    @Test
    fun given_a_load_failure_when_rendered_then_retry_invokes_the_callback() {
        var retried = false
        composeTestRule.setContent {
            MapErrorBanner(message = "network", onRetry = { retried = true })
        }

        composeTestRule.onNodeWithTag("map_retry_button").performClick()

        assert(retried)
    }
}
