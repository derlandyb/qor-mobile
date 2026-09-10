package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.qualorock.androidApp.ui.viewmodel.HubViewModel
import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import domain.event.MapBounds
import domain.event.usecase.ListUpcomingEvents
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

/** In-file fake, same shape as other screens' render-test fakes — keeps this independent of Koin. */
private class FakeHubScreenRepository(
    private val eventsByCity: Map<City, Result<EventPage>>,
) : EventRepository {
    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage {
        val key = city ?: error("HubScreen must always query by a specific city")
        return (eventsByCity[key] ?: error("no response configured for $key")).getOrThrow()
    }

    override suspend fun findById(id: String): EventDetail = error("not used by HubScreenTest")

    override suspend fun getMapEvents(city: City?, bounds: MapBounds?): List<Event> =
        error("not used by HubScreenTest")
}

private fun sampleEvent(id: String, title: String, city: City) = Event(
    id = id,
    title = title,
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = city,
    genre = "Rock",
    address = "Rua X, 100",
    isFree = true,
    ticketUrl = null,
)

private fun viewModel(eventsByCity: Map<City, Result<EventPage>>): HubViewModel =
    HubViewModel(ListUpcomingEvents(FakeHubScreenRepository(eventsByCity)))

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HubScreenTest {

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
    fun `GIVEN a city with published events THEN only that city's header and events render`() {
        val page = EventPage(listOf(sampleEvent("e1", "Show em Serra", City.Serra)), nextCursor = null)
        composeTestRule.setContent {
            HubScreen(city = City.Serra, onEventClick = {}, viewModel = viewModel(mapOf(City.Serra to Result.success(page))))
        }

        composeTestRule.onNodeWithText("Serra").assertExists()
        composeTestRule.onNodeWithText("Show em Serra").assertExists()
    }

    @Test
    fun `GIVEN a Hub event card WHEN tapped THEN onEventClick fires with its id`() {
        val page = EventPage(listOf(sampleEvent("e1", "Show em Serra", City.Serra)), nextCursor = null)
        var clickedId: String? = null
        composeTestRule.setContent {
            HubScreen(
                city = City.Serra,
                onEventClick = { clickedId = it },
                viewModel = viewModel(mapOf(City.Serra to Result.success(page))),
            )
        }

        composeTestRule.onNodeWithText("Show em Serra").performClick()

        assert(clickedId == "e1")
    }

    @Test
    fun `GIVEN a city has zero published events THEN the empty state is shown, not a blank page`() {
        composeTestRule.setContent {
            HubScreen(
                city = City.Cariacica,
                onEventClick = {},
                viewModel = viewModel(mapOf(City.Cariacica to Result.success(EventPage(emptyList(), nextCursor = null)))),
            )
        }

        composeTestRule.onNodeWithText("Cariacica").assertExists()
        composeTestRule.onNodeWithText("Nenhum evento encontrado").assertExists()
    }

    @Test
    fun `GIVEN the events call fails THEN the error message and retry CTA are shown`() {
        composeTestRule.setContent {
            HubScreen(
                city = City.Vitoria,
                onEventClick = {},
                viewModel = viewModel(mapOf(City.Vitoria to Result.failure(RuntimeException("boom")))),
            )
        }

        composeTestRule.onNodeWithText("Não foi possível carregar os eventos deste hub. Tente novamente.").assertExists()
        composeTestRule.onNodeWithText("Tentar novamente").assertExists()
    }
}
