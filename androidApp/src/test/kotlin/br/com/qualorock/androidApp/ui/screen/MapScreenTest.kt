package br.com.qualorock.androidApp.ui.screen

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.com.qualorock.androidApp.ui.viewmodel.MapViewModel
import domain.enum.City
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPage
import domain.event.EventRepository
import domain.event.MapBounds
import domain.event.usecase.GetMapEvents
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

/**
 * In-file fake, same shape as other screens' render-test fakes. This screen's `Content` state
 * (the actual `maps-compose` `GoogleMap`/`Marker` render) is not exercised here — see
 * [MapScreen]'s own KDoc: `EventDetailScreenTest` never renders its Located/`GoogleMap` branch
 * either, since `maps-compose` needs real Play-Services internals Robolectric doesn't provide.
 * [ToMapPinsTest] and `MapViewModelTest` cover the pin-data/tap-target logic instead.
 */
private class FakeMapScreenRepository(private val result: Result<List<Event>>) : EventRepository {
    override suspend fun findUpcoming(city: City?, genre: String?, cursor: String?): EventPage =
        error("not used by MapScreenTest")

    override suspend fun findById(id: String): EventDetail = error("not used by MapScreenTest")

    override suspend fun getMapEvents(city: City?, bounds: MapBounds?): List<Event> = result.getOrThrow()
}

private fun viewModel(result: Result<List<Event>>): MapViewModel =
    MapViewModel(GetMapEvents(FakeMapScreenRepository(result)))

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MapScreenTest {

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
    fun `GIVEN the map query fails THEN the error message and retry CTA are shown`() {
        composeTestRule.setContent {
            MapScreen(onEventClick = {}, viewModel = viewModel(Result.failure(RuntimeException("boom"))))
        }

        composeTestRule.onNodeWithText("Não foi possível carregar o mapa. Tente novamente.").assertExists()
        composeTestRule.onNodeWithText("Tentar novamente").assertExists()
    }
}
