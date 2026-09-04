package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.com.qualorock.androidApp.ui.viewmodel.EventDetailViewModel
import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventPromoterContact
import domain.event.EventRepository
import domain.event.usecase.GetEventDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** In-file fake, same shape as other screens' render-test fakes — keeps this independent of Koin. */
private class FakeEventDetailRepository(private val result: Result<EventDetail>) : EventRepository {
    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage =
        error("not used by EventDetailScreenTest")

    override suspend fun findById(id: String): EventDetail = result.getOrThrow()
}

private fun sampleEvent(isFree: Boolean = true, ticketUrl: String? = null) = Event(
    id = "e1",
    title = "Show de Rock",
    description = "Uma noite de rock autoral.",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = City.Vitoria,
    genre = "Rock",
    address = "Rua X, 100 - Vitória",
    isFree = isFree,
    ticketUrl = ticketUrl,
)

private fun viewModel(detail: EventDetail): EventDetailViewModel =
    EventDetailViewModel(GetEventDetails(FakeEventDetailRepository(Result.success(detail))))

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class EventDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN an Active free event WHEN rendered THEN title, description, address and the free label are shown`() {
        composeTestRule.setContent {
            EventDetailScreen(eventId = "e1", viewModel = viewModel(EventDetail.Active(sampleEvent(isFree = true))))
        }

        composeTestRule.onNodeWithText("Show de Rock").assertExists()
        composeTestRule.onNodeWithText("Uma noite de rock autoral.").assertExists()
        composeTestRule.onNodeWithText("Rua X, 100 - Vitória").assertExists()
        composeTestRule.onNodeWithText("Gratuito").assertExists()
    }

    @Test
    fun `GIVEN a Cancelled event WHEN rendered THEN only the cancelled banner is shown`() {
        composeTestRule.setContent {
            EventDetailScreen(eventId = "e1", viewModel = viewModel(EventDetail.Cancelled(sampleEvent())))
        }

        composeTestRule.onNodeWithText("Evento cancelado").assertExists()
        composeTestRule.onNodeWithText("Uma noite de rock autoral.").assertDoesNotExist()
    }

    @Test
    fun `GIVEN an Ended event WHEN rendered THEN only the ended banner is shown`() {
        composeTestRule.setContent {
            EventDetailScreen(eventId = "e1", viewModel = viewModel(EventDetail.Ended(sampleEvent())))
        }

        composeTestRule.onNodeWithText("Este evento já aconteceu").assertExists()
        composeTestRule.onNodeWithText("Uma noite de rock autoral.").assertDoesNotExist()
    }

    @Test
    fun `GIVEN a free Active event WHEN rendered THEN no ticket button is shown`() {
        composeTestRule.setContent {
            EventDetailScreen(eventId = "e1", viewModel = viewModel(EventDetail.Active(sampleEvent(isFree = true))))
        }

        composeTestRule.onNodeWithText("Comprar ingresso").assertDoesNotExist()
    }

    @Test
    fun `GIVEN a paid Active event WHEN rendered THEN the ticket button is shown`() {
        composeTestRule.setContent {
            EventDetailScreen(
                eventId = "e1",
                viewModel = viewModel(EventDetail.Active(sampleEvent(isFree = false, ticketUrl = "https://tickets.example.com/e1"))),
            )
        }

        composeTestRule.onNodeWithText("Pago").assertExists()
        composeTestRule.onNodeWithText("Comprar ingresso").assertExists()
    }

    @Test
    fun `GIVEN the ticket link fails to open WHEN the ticket button is tapped THEN an inline error message is shown`() {
        composeTestRule.setContent {
            EventDetailScreen(
                eventId = "e1",
                viewModel = viewModel(EventDetail.Active(sampleEvent(isFree = false, ticketUrl = "broken://url"))),
                launchIntent = { _, _ -> error("simulated unresolvable intent") },
            )
        }

        composeTestRule.onNodeWithText("Comprar ingresso").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Não foi possível abrir o link do ingresso.").assertExists()
    }

    @Test
    fun `GIVEN the ticket link opens successfully WHEN tapped THEN the launcher receives an ACTION_VIEW intent for the ticket url`() {
        var receivedIntent: Intent? = null
        composeTestRule.setContent {
            EventDetailScreen(
                eventId = "e1",
                viewModel = viewModel(EventDetail.Active(sampleEvent(isFree = false, ticketUrl = "https://tickets.example.com/e1"))),
                launchIntent = { _, intent -> receivedIntent = intent },
            )
        }

        composeTestRule.onNodeWithText("Comprar ingresso").performScrollTo().performClick()

        assertEquals(Intent.ACTION_VIEW, receivedIntent?.action)
        assertEquals("https://tickets.example.com/e1", receivedIntent?.data.toString())
    }

    @Test
    fun `GIVEN the map button is tapped THEN the launcher receives a geo ACTION_VIEW intent built from the address`() {
        var receivedIntent: Intent? = null
        composeTestRule.setContent {
            EventDetailScreen(
                eventId = "e1",
                viewModel = viewModel(EventDetail.Active(sampleEvent())),
                launchIntent = { _, intent -> receivedIntent = intent },
            )
        }

        composeTestRule.onNodeWithText("Abrir no mapa").performScrollTo().performClick()

        assertEquals(Intent.ACTION_VIEW, receivedIntent?.action)
        assertEquals("geo", receivedIntent?.data?.scheme)
    }

    @Test
    fun `GIVEN the share button is tapped THEN the launcher receives a chooser wrapping an ACTION_SEND intent`() {
        var receivedIntent: Intent? = null
        composeTestRule.setContent {
            EventDetailScreen(
                eventId = "e1",
                viewModel = viewModel(EventDetail.Active(sampleEvent())),
                launchIntent = { _, intent -> receivedIntent = intent },
            )
        }

        composeTestRule.onNodeWithText("Compartilhar").performScrollTo().performClick()

        assert(receivedIntent?.action == Intent.ACTION_CHOOSER)
    }

    @Test
    fun `GIVEN a promoter with only a phone number WHEN rendered THEN only the call button is shown for that contact`() {
        val detail = EventDetail.Active(
            sampleEvent(),
            promoters = listOf(EventPromoterContact(name = "DJ Ana", phone = "27999999999", email = null, instagram = null, tiktok = null)),
        )
        composeTestRule.setContent {
            EventDetailScreen(eventId = "e1", viewModel = viewModel(detail))
        }

        composeTestRule.onNodeWithText("DJ Ana").assertExists()
        composeTestRule.onNodeWithText("Ligar").assertExists()
        composeTestRule.onNodeWithText("Enviar e-mail").assertDoesNotExist()
        composeTestRule.onNodeWithText("Ver Instagram").assertDoesNotExist()
        composeTestRule.onNodeWithText("TikTok").assertDoesNotExist()
    }

    @Test
    fun `GIVEN a promoter with every contact field WHEN rendered THEN every contact button is shown`() {
        val detail = EventDetail.Active(
            sampleEvent(),
            promoters = listOf(
                EventPromoterContact(
                    name = "DJ Ana",
                    phone = "27999999999",
                    email = "ana@example.com",
                    instagram = "djana",
                    tiktok = "djana",
                ),
            ),
        )
        composeTestRule.setContent {
            EventDetailScreen(eventId = "e1", viewModel = viewModel(detail))
        }

        composeTestRule.onNodeWithText("Ligar").assertExists()
        composeTestRule.onNodeWithText("Enviar e-mail").assertExists()
        composeTestRule.onNodeWithText("Ver Instagram").assertExists()
        composeTestRule.onNodeWithText("TikTok").assertExists()
    }
}
