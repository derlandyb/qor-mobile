package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.qualorock.androidApp.ui.viewmodel.FavoritesViewModel
import domain.enum.City
import domain.event.Event
import domain.favorite.FavoriteRepository
import domain.favorite.usecase.ListFavorites
import domain.favorite.usecase.ToggleFavorite
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
private class FakeFavoritesScreenRepository(
    private val listResult: Result<List<Event>>,
    private val toggleResult: Result<Boolean> = Result.success(false),
) : FavoriteRepository {
    override suspend fun toggle(eventId: String): Boolean = toggleResult.getOrThrow()
    override suspend fun list(): List<Event> = listResult.getOrThrow()
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

private fun viewModel(
    listResult: Result<List<Event>>,
    toggleResult: Result<Boolean> = Result.success(false),
): FavoritesViewModel {
    val repository = FakeFavoritesScreenRepository(listResult, toggleResult)
    return FavoritesViewModel(ListFavorites(repository), ToggleFavorite(repository))
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FavoritesScreenTest {

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
    fun `GIVEN favorited events THEN the title and events are rendered`() {
        composeTestRule.setContent {
            FavoritesScreen(
                onEventClick = {},
                viewModel = viewModel(Result.success(listOf(sampleEvent("e1", "Show da Banda X")))),
            )
        }

        composeTestRule.onNodeWithText("Meus Favoritos").assertExists()
        composeTestRule.onNodeWithText("Show da Banda X").assertExists()
    }

    @Test
    fun `GIVEN a favorited event WHEN its card is tapped THEN onEventClick fires with its id`() {
        var clickedId: String? = null
        composeTestRule.setContent {
            FavoritesScreen(
                onEventClick = { clickedId = it },
                viewModel = viewModel(Result.success(listOf(sampleEvent("e1", "Show da Banda X")))),
            )
        }

        composeTestRule.onNodeWithText("Show da Banda X").performClick()

        assert(clickedId == "e1")
    }

    @Test
    fun `GIVEN a favorited event WHEN its remove control is tapped THEN it disappears from the list`() {
        composeTestRule.setContent {
            FavoritesScreen(
                onEventClick = {},
                viewModel = viewModel(Result.success(listOf(sampleEvent("e1", "Show da Banda X")))),
            )
        }

        composeTestRule.onNodeWithContentDescription("Remover Show da Banda X dos favoritos").performClick()

        composeTestRule.onNodeWithText("Show da Banda X").assertDoesNotExist()
        composeTestRule.onNodeWithText("Você ainda não favoritou nenhum evento.").assertExists()
    }

    @Test
    fun `GIVEN no favorited events THEN the empty state is shown`() {
        composeTestRule.setContent {
            FavoritesScreen(onEventClick = {}, viewModel = viewModel(Result.success(emptyList())))
        }

        composeTestRule.onNodeWithText("Você ainda não favoritou nenhum evento.").assertExists()
    }

    @Test
    fun `GIVEN the favorites call fails THEN the error message and retry CTA are shown`() {
        composeTestRule.setContent {
            FavoritesScreen(onEventClick = {}, viewModel = viewModel(Result.failure(RuntimeException("boom"))))
        }

        composeTestRule.onNodeWithText("Não foi possível carregar seus favoritos. Tente novamente.").assertExists()
        composeTestRule.onNodeWithText("Tentar novamente").assertExists()
    }
}
