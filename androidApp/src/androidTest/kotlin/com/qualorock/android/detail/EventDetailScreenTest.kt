package com.qualorock.android.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.qualorock.shared.detail.EventDetailUiState
import com.qualorock.shared.domain.AgeRating
import com.qualorock.shared.domain.BannerStatus
import com.qualorock.shared.domain.Event
import com.qualorock.shared.domain.EventStatus
import com.qualorock.shared.domain.Price
import com.qualorock.shared.domain.Promoter
import com.qualorock.shared.domain.Venue
import com.qualorock.shared.domain.VerificationStatus
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class EventDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun event(
        bannerStatus: BannerStatus? = null,
        ticketUrl: String? = "https://www.sympla.com.br/",
        description: String = "Uma noite especial de rock, com muita energia e diversão para todos.",
        staticMapUrl: String? = "https://maps.googleapis.com/maps/api/staticmap?center=1,1",
        address: String? = "Rua Rio Branco, 100",
        promoter: Promoter? = Promoter(id = "9", name = "Produtora XYZ", verificationStatus = VerificationStatus.VERIFIED),
    ) = Event(
        id = "1",
        title = "Show de Rock",
        description = description,
        startDateTime = Instant.parse("2026-08-16T22:00:00Z"),
        venue =
            Venue(
                id = "v1",
                name = "Matrix",
                city = "Vitória",
                address = address,
                latitude = if (staticMapUrl != null) -20.3103 else null,
                longitude = if (staticMapUrl != null) -40.3211 else null,
                staticMapUrl = staticMapUrl,
                verificationStatus = VerificationStatus.VERIFIED,
            ),
        city = "Vitória",
        price = Price(isFree = false, min = 60.0, max = 60.0, currency = "BRL"),
        ageRating = AgeRating.EIGHTEEN,
        ticketUrl = if (bannerStatus == null) ticketUrl else null,
        status = if (bannerStatus == BannerStatus.CANCELLED) EventStatus.CANCELLED else EventStatus.PUBLISHED,
        bannerStatus = bannerStatus,
        promoter = promoter,
    )

    @Test
    fun given_a_cancelled_event_when_the_detail_screen_renders_then_the_cancelled_banner_shows_and_the_ticket_link_is_absent() {
        composeTestRule.setContent {
            EventDetailScreen(state = EventDetailUiState.Loaded(event(bannerStatus = BannerStatus.CANCELLED)), onRetry = {}, onBack = {})
        }

        composeTestRule.onNodeWithTag("status_banner_cancelled").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ticket_link").assertDoesNotExist()
    }

    @Test
    fun given_a_finished_event_when_the_detail_screen_renders_then_the_finished_banner_shows_and_the_ticket_link_is_absent() {
        composeTestRule.setContent {
            EventDetailScreen(state = EventDetailUiState.Loaded(event(bannerStatus = BannerStatus.FINISHED)), onRetry = {}, onBack = {})
        }

        composeTestRule.onNodeWithTag("status_banner_finished").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ticket_link").assertDoesNotExist()
    }

    @Test
    fun given_a_normal_published_event_when_the_detail_screen_renders_then_no_banner_shows_and_the_ticket_link_is_present() {
        composeTestRule.setContent {
            EventDetailScreen(state = EventDetailUiState.Loaded(event()), onRetry = {}, onBack = {})
        }

        composeTestRule.onNodeWithTag("status_banner_cancelled").assertDoesNotExist()
        composeTestRule.onNodeWithTag("status_banner_finished").assertDoesNotExist()
        composeTestRule.onNodeWithTag("ticket_link").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action_save").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action_share").assertIsDisplayed()
    }

    @Test
    fun given_a_venue_with_coordinates_when_the_detail_screen_renders_then_the_static_map_image_is_shown() {
        composeTestRule.setContent {
            EventDetailScreen(state = EventDetailUiState.Loaded(event()), onRetry = {}, onBack = {})
        }

        composeTestRule.onNodeWithTag("location_map").assertIsDisplayed()
    }

    @Test
    fun given_a_venue_with_only_an_address_when_the_detail_screen_renders_then_only_address_text_is_shown() {
        composeTestRule.setContent {
            EventDetailScreen(state = EventDetailUiState.Loaded(event(staticMapUrl = null)), onRetry = {}, onBack = {})
        }

        composeTestRule.onNodeWithTag("location_map").assertDoesNotExist()
        composeTestRule.onNodeWithTag("location_address").assertIsDisplayed()
    }

    @Test
    fun given_a_venue_with_no_coordinates_and_no_address_when_the_detail_screen_renders_then_the_location_section_is_omitted() {
        composeTestRule.setContent {
            EventDetailScreen(
                state = EventDetailUiState.Loaded(event(staticMapUrl = null, address = null)),
                onRetry = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("location_section").assertDoesNotExist()
    }

    @Test
    fun given_a_long_description_when_the_detail_screen_renders_then_the_full_text_is_shown_untruncated() {
        val longDescription = "Parágrafo longo. ".repeat(50)
        composeTestRule.setContent {
            EventDetailScreen(state = EventDetailUiState.Loaded(event(description = longDescription)), onRetry = {}, onBack = {})
        }

        composeTestRule.onNodeWithText(longDescription, substring = true).assertIsDisplayed()
    }

    @Test
    fun given_a_not_found_state_when_the_detail_screen_renders_then_the_not_found_message_is_shown() {
        composeTestRule.setContent {
            EventDetailScreen(state = EventDetailUiState.NotFound, onRetry = {}, onBack = {})
        }

        composeTestRule.onNodeWithTag("not_found_state").assertIsDisplayed()
    }

    @Test
    fun given_a_load_error_state_when_retry_is_tapped_then_onRetry_is_invoked() {
        var retried = false
        composeTestRule.setContent {
            EventDetailScreen(state = EventDetailUiState.LoadError("network"), onRetry = { retried = true }, onBack = {})
        }

        composeTestRule.onNodeWithTag("retry_button").performClick()

        assert(retried)
    }
}
