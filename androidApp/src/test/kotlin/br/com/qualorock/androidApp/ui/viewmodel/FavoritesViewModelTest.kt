package br.com.qualorock.androidApp.ui.viewmodel

import domain.enum.City
import domain.event.Event
import domain.favorite.FavoriteRepository
import domain.favorite.usecase.ListFavorites
import domain.favorite.usecase.ToggleFavorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * In-file fake, same shape as other screens' in-file repository fakes — `androidApp` can't see
 * `shared`'s `commonTest` `FakeFavoriteRepository`.
 */
private class FakeFavoritesRepository(
    private val listResult: Result<List<Event>>,
    private val toggleResult: Result<Boolean> = Result.success(false),
) : FavoriteRepository {
    var toggledEventIds = mutableListOf<String>()

    override suspend fun toggle(eventId: String): Boolean {
        toggledEventIds.add(eventId)
        return toggleResult.getOrThrow()
    }

    override suspend fun list(): List<Event> = listResult.getOrThrow()
}

private fun sampleEvent(id: String) = Event(
    id = id,
    title = "Show $id",
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = City.Vitoria,
    genre = "Rock",
    address = "Rua X, 100",
    isFree = true,
    ticketUrl = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeFavoritesRepository): FavoritesViewModel =
        FavoritesViewModel(ListFavorites(repository), ToggleFavorite(repository))

    @Test
    fun `GIVEN favorited events exist WHEN the screen loads THEN they are shown`() = runTest {
        val repository = FakeFavoritesRepository(Result.success(listOf(sampleEvent("e1"), sampleEvent("e2"))))
        val vm = viewModel(repository)
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        check(state is FavoritesUiState.Content)
        assertEquals(listOf("e1", "e2"), state.events.map { it.id })
    }

    @Test
    fun `GIVEN no favorited events exist WHEN the screen loads THEN the empty state is shown`() = runTest {
        val repository = FakeFavoritesRepository(Result.success(emptyList()))
        val vm = viewModel(repository)
        dispatcher.scheduler.runCurrent()

        assertEquals(FavoritesUiState.Empty, vm.uiState.value)
    }

    @Test
    fun `GIVEN the list call fails WHEN the screen loads THEN Error is surfaced`() = runTest {
        val repository = FakeFavoritesRepository(Result.failure(RuntimeException("boom")))
        val vm = viewModel(repository)
        dispatcher.scheduler.runCurrent()

        assertEquals(FavoritesUiState.Error, vm.uiState.value)
    }

    @Test
    fun `GIVEN a favorited event WHEN it is un-favorited from the list THEN it is removed without a reload`() = runTest {
        val repository = FakeFavoritesRepository(
            listResult = Result.success(listOf(sampleEvent("e1"), sampleEvent("e2"))),
            toggleResult = Result.success(false),
        )
        val vm = viewModel(repository)
        dispatcher.scheduler.runCurrent()

        vm.onUnfavorite("e1")

        // Removed immediately (optimistic), before the toggle call's response resolves.
        val optimisticState = vm.uiState.value
        check(optimisticState is FavoritesUiState.Content)
        assertEquals(listOf("e2"), optimisticState.events.map { it.id })

        dispatcher.scheduler.runCurrent()

        val settledState = vm.uiState.value
        check(settledState is FavoritesUiState.Content)
        assertEquals(listOf("e2"), settledState.events.map { it.id })
        assertEquals(listOf("e1"), repository.toggledEventIds)
    }

    @Test
    fun `GIVEN the toggle call fails WHEN un-favoriting THEN the event is rolled back into the list`() = runTest {
        val repository = FakeFavoritesRepository(
            listResult = Result.success(listOf(sampleEvent("e1"))),
            toggleResult = Result.failure(RuntimeException("network error")),
        )
        val vm = viewModel(repository)
        dispatcher.scheduler.runCurrent()

        vm.onUnfavorite("e1")
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        check(state is FavoritesUiState.Content)
        assertEquals(listOf("e1"), state.events.map { it.id })
    }

    @Test
    fun `GIVEN the server reports still favorited WHEN un-favoriting THEN the server's final state wins`() = runTest {
        // Edge case: unfavoriting resolves to the server's actual final state, not the UI's last
        // optimistic guess — here the toggle response says `favorited = true`, so the event must
        // come back into the list even though it was optimistically removed.
        val repository = FakeFavoritesRepository(
            listResult = Result.success(listOf(sampleEvent("e1"))),
            toggleResult = Result.success(true),
        )
        val vm = viewModel(repository)
        dispatcher.scheduler.runCurrent()

        vm.onUnfavorite("e1")
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        check(state is FavoritesUiState.Content)
        assertEquals(listOf("e1"), state.events.map { it.id })
    }
}
