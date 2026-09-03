package br.com.qor.androidApp.ui.components

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import domain.enum.City
import domain.event.Event
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp")
class EventCardRenderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val event = Event(
        id = "1",
        title = "Noite do Rock",
        description = "Uma noite de rock autoral",
        coverImageUrl = null,
        startsAt = "2026-09-20T22:00:00Z",
        city = City.Vitoria,
        genre = "Rock",
        address = "Rua das Flores, 100",
        isFree = false,
        ticketUrl = "https://example.com/ingresso",
    )

    @Test
    fun `GIVEN an event WHEN EventCard is rendered THEN title, date, genre and address are displayed`() {
        composeTestRule.setContent {
            EventCard(event = event, onClick = {}, onMapClick = {})
        }

        composeTestRule.onNodeWithText("Noite do Rock").assertExists()
        composeTestRule.onNodeWithText("20").assertExists()
        composeTestRule.onNodeWithText("SET").assertExists()
        composeTestRule.onNodeWithText("ROCK").assertExists()
        composeTestRule.onNodeWithText("Rua das Flores, 100").assertExists()
    }

    @Test
    fun `GIVEN a card WHEN it is clicked THEN onClick fires`() {
        var clicked = false
        composeTestRule.setContent {
            EventCard(event = event, onClick = { clicked = true }, onMapClick = {})
        }

        composeTestRule.onNodeWithText("Noite do Rock").performClick()

        assert(clicked)
    }

    @Test
    fun `GIVEN the map CTA WHEN it is clicked THEN onMapClick fires without triggering onClick`() {
        var cardClicked = false
        var mapClicked = false
        composeTestRule.setContent {
            EventCard(event = event, onClick = { cardClicked = true }, onMapClick = { mapClicked = true })
        }

        composeTestRule.onNodeWithTag(MapaCtaTestTag).performClick()

        assert(mapClicked)
        assert(!cardClicked)
    }
}
