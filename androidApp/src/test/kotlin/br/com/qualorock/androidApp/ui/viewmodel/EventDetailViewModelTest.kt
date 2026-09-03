package br.com.qualorock.androidApp.ui.viewmodel

import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPromoterContact
import domain.event.EventRepository
import domain.event.EventPage
import domain.event.usecase.GetEventDetails
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
import kotlin.test.assertTrue

/**
 * In-file [EventRepository] fake, same shape as `HomeFeedViewModelTest`'s — `androidApp` only
 * depends on `shared`'s main artifact, not `commonTest`.
 */
private class FakeEventDetailRepository(
    private val result: Result<EventDetail>,
) : EventRepository {
    var lastRequestedId: String? = null

    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage =
        error("not used by EventDetailViewModelTest")

    override suspend fun findById(id: String): EventDetail {
        lastRequestedId = id
        return result.getOrThrow()
    }
}

private fun sampleEvent(id: String = "e1") = Event(
    id = id,
    title = "Show de Rock",
    description = "Uma noite de rock",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = City.Vitoria,
    genre = "Rock",
    address = "Rua X, 100",
    isFree = false,
    ticketUrl = "https://tickets.example.com/e1",
)

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN it is constructed THEN the initial state is Loading`() = runTest {
        val vm = EventDetailViewModel(GetEventDetails(FakeEventDetailRepository(Result.success(EventDetail.Active(sampleEvent())))))

        assertEquals(EventDetailUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `GIVEN a valid eventId WHEN load succeeds THEN Content wraps the returned EventDetail`() = runTest {
        val detail = EventDetail.Active(sampleEvent(), promoters = listOf(EventPromoterContact("DJ Ana", "27999999999", null, null, null)))
        val repository = FakeEventDetailRepository(Result.success(detail))
        val vm = EventDetailViewModel(GetEventDetails(repository))

        vm.load("e1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(EventDetailUiState.Content(detail), vm.uiState.value)
        assertEquals("e1", repository.lastRequestedId)
    }

    @Test
    fun `GIVEN load is called THEN state resets to Loading before the result arrives`() = runTest {
        val repository = FakeEventDetailRepository(Result.success(EventDetail.Active(sampleEvent())))
        val vm = EventDetailViewModel(GetEventDetails(repository))
        vm.load("e1")
        dispatcher.scheduler.advanceUntilIdle()

        vm.load("e1")

        assertEquals(EventDetailUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `GIVEN the repository throws WHEN load is called THEN state becomes Error`() = runTest {
        val repository = FakeEventDetailRepository(Result.failure(RuntimeException("boom")))
        val vm = EventDetailViewModel(GetEventDetails(repository))

        vm.load("e1")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is EventDetailUiState.Error)
    }

    @Test
    fun `GIVEN a Cancelled EventDetail WHEN load succeeds THEN Content wraps it unmodified`() = runTest {
        val detail = EventDetail.Cancelled(sampleEvent())
        val repository = FakeEventDetailRepository(Result.success(detail))
        val vm = EventDetailViewModel(GetEventDetails(repository))

        vm.load("e1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(EventDetailUiState.Content(detail), vm.uiState.value)
    }
}
