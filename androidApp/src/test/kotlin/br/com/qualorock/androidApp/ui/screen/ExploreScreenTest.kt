package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.qualorock.androidApp.ui.viewmodel.ExploreViewModel
import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import domain.event.PollingCoordinator
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

/** In-file fake, same shape as `HomeFeedScreenTest`'s — keeps this render test independent of Koin. */
private class FakeExploreScreenEventRepository(
    private val responses: MutableList<Result<EventPage>>,
) : EventRepository {
    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage {
        val next = if (responses.size > 1) responses.removeAt(0) else responses.first()
        return next.getOrThrow()
    }

    override suspend fun findById(id: String): EventDetail = error("not used by ExploreScreenTest")
}

private fun sampleEvent(id: String, title: String, city: City = City.Vitoria) = Event(
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

private fun viewModel(responses: List<Result<EventPage>>): ExploreViewModel {
    val repository = FakeExploreScreenEventRepository(responses.toMutableList())
    val listUpcomingEvents = ListUpcomingEvents(repository)
    return ExploreViewModel(listUpcomingEvents, PollingCoordinator(listUpcomingEvents))
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ExploreScreenTest {

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
    fun `GIVEN events are loaded THEN the event titles and filter bars are rendered`() {
        val page = EventPage(listOf(sampleEvent("e1", "Show da Banda X")), nextCursor = null)
        composeTestRule.setContent {
            ExploreScreen(onEventClick = {}, viewModel = viewModel(listOf(Result.success(page))))
        }

        composeTestRule.onNodeWithText("Show da Banda X").assertExists()
        // "SERRA"/"SAMBA", not the sample event's own city ("VITÓRIA")/genre ("ROCK") — its
        // EventCard already renders those as badges, so asserting on them would match two nodes
        // (card badge + filter chip) instead of confirming the filter bars themselves render.
        composeTestRule.onNodeWithText("SERRA").assertExists()
        composeTestRule.onNodeWithText("SAMBA").assertExists()
    }

    @Test
    fun `GIVEN a loaded event WHEN its card is tapped THEN onEventClick fires with its id`() {
        val page = EventPage(listOf(sampleEvent("e1", "Show da Banda X")), nextCursor = null)
        var clickedId: String? = null
        composeTestRule.setContent {
            ExploreScreen(
                onEventClick = { clickedId = it },
                viewModel = viewModel(listOf(Result.success(page))),
            )
        }

        composeTestRule.onNodeWithText("Show da Banda X").performClick()

        assert(clickedId == "e1")
    }

    @Test
    fun `GIVEN a city filter WHEN tapped THEN the filtered event list is rendered`() {
        val settled = EventPage(listOf(sampleEvent("e1", "Show da Banda X")), nextCursor = null)
        val filtered = EventPage(listOf(sampleEvent("e2", "Show em Serra", city = City.Serra)), nextCursor = null)
        composeTestRule.setContent {
            ExploreScreen(
                onEventClick = {},
                viewModel = viewModel(listOf(Result.success(settled), Result.success(settled), Result.success(filtered))),
            )
        }

        composeTestRule.onNodeWithText("SERRA").performClick()

        composeTestRule.onNodeWithText("Show em Serra").assertExists()
    }

    @Test
    fun `GIVEN filters yield no matches THEN the empty state shows a clear-filters CTA`() {
        val settled = EventPage(listOf(sampleEvent("e1", "Show da Banda X")), nextCursor = null)
        val empty = EventPage(emptyList(), nextCursor = null)
        composeTestRule.setContent {
            ExploreScreen(
                onEventClick = {},
                viewModel = viewModel(listOf(Result.success(settled), Result.success(settled), Result.success(empty))),
            )
        }

        composeTestRule.onNodeWithText("SERRA").performClick()

        composeTestRule.onNodeWithText("Nenhum evento encontrado para estes filtros.").assertExists()
        composeTestRule.onNodeWithText("Limpar filtros").assertExists()
    }

    @Test
    fun `GIVEN the empty state with filters WHEN clear filters is tapped THEN the default list returns`() {
        val settled = EventPage(listOf(sampleEvent("e1", "Show da Banda X")), nextCursor = null)
        val empty = EventPage(emptyList(), nextCursor = null)
        // Each filter change fires two fetches (the direct reload + PollingCoordinator's own
        // immediate restart fetch, per HomeFeedViewModel's KDoc), so "empty" must appear twice in a
        // row here or the second call would fall through to the trailing "settled" prematurely.
        composeTestRule.setContent {
            ExploreScreen(
                onEventClick = {},
                viewModel = viewModel(
                    listOf(
                        Result.success(settled),
                        Result.success(settled),
                        Result.success(empty),
                        Result.success(empty),
                        Result.success(settled),
                    ),
                ),
            )
        }
        composeTestRule.onNodeWithText("SERRA").performClick()
        composeTestRule.onNodeWithText("Limpar filtros").assertExists()

        composeTestRule.onNodeWithText("Limpar filtros").performClick()

        composeTestRule.onNodeWithText("Show da Banda X").assertExists()
    }
}
