package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import domain.event.PollingCoordinator
import domain.event.usecase.ListUpcomingEvents
import br.com.qualorock.androidApp.ui.viewmodel.HomeFeedViewModel
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

/** In-file fake, same shape as `HomeFeedViewModelTest`'s — keeps this render test independent of Koin. */
private class FakeHomeFeedScreenEventRepository(
    private val responses: MutableList<Result<EventPage>>,
) : EventRepository {
    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage {
        val next = if (responses.size > 1) responses.removeAt(0) else responses.first()
        return next.getOrThrow()
    }

    override suspend fun findById(id: String): EventDetail = error("not used by HomeFeedScreenTest")
}

private fun sampleEvent(id: String, title: String) = Event(
    id = id,
    title = title,
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = City.Vitoria,
    genre = "Rock",
    address = "Rua X, 100",
    isFree = true,
    ticketUrl = null,
)

private fun viewModel(responses: List<Result<EventPage>>): HomeFeedViewModel {
    val repository = FakeHomeFeedScreenEventRepository(responses.toMutableList())
    val listUpcomingEvents = ListUpcomingEvents(repository)
    return HomeFeedViewModel(listUpcomingEvents, PollingCoordinator(listUpcomingEvents))
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HomeFeedScreenTest {

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
    fun `GIVEN events are loaded THEN the event titles are rendered`() {
        val page = EventPage(listOf(sampleEvent("e1", "Show da Banda X")), nextCursor = null)
        composeTestRule.setContent {
            HomeFeedScreen(onEventClick = {}, viewModel = viewModel(listOf(Result.success(page))))
        }

        composeTestRule.onNodeWithText("Show da Banda X").assertExists()
    }

    @Test
    fun `GIVEN a loaded event WHEN its card is tapped THEN onEventClick fires with its id`() {
        val page = EventPage(listOf(sampleEvent("e1", "Show da Banda X")), nextCursor = null)
        var clickedId: String? = null
        composeTestRule.setContent {
            HomeFeedScreen(
                onEventClick = { clickedId = it },
                viewModel = viewModel(listOf(Result.success(page))),
            )
        }

        composeTestRule.onNodeWithText("Show da Banda X").performClick()

        assert(clickedId == "e1")
    }

    @Test
    fun `GIVEN no events are returned THEN the empty state message is shown`() {
        composeTestRule.setContent {
            HomeFeedScreen(onEventClick = {}, viewModel = viewModel(listOf(Result.success(EventPage(emptyList(), null)))))
        }

        composeTestRule.onNodeWithText("Nenhum evento encontrado").assertExists()
    }

    @Test
    fun `GIVEN the initial load fails THEN the error message and retry CTA are shown`() {
        composeTestRule.setContent {
            HomeFeedScreen(onEventClick = {}, viewModel = viewModel(listOf(Result.failure(RuntimeException("boom")))))
        }

        composeTestRule.onNodeWithText("Não foi possível carregar os eventos. Tente novamente.").assertExists()
        composeTestRule.onNodeWithText("Tentar novamente").assertExists()
    }
}
