package domain.event

import data.QorConfig
import domain.enum.City
import domain.event.usecase.ListUpcomingEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MillisPerSecond = 1_000L

/**
 * Shared polling loop for the live event list (DISC-04), wrapping [ListUpcomingEvents]. Both
 * platform UIs subscribe to the same [events] flow — one fetch feeds both the periodic tick
 * and any manual [refreshNow] call.
 */
class PollingCoordinator(private val listUpcomingEvents: ListUpcomingEvents) {

    private val _events = MutableStateFlow(EventPage(emptyList(), null))
    val events: StateFlow<EventPage> = _events.asStateFlow()

    private var pollingJob: Job? = null
    private var lastCity: City? = null
    private var lastGenre: String? = null

    fun start(scope: CoroutineScope, city: City? = null, genre: String? = null) {
        lastCity = city
        lastGenre = genre
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (true) {
                fetch()
                delay(QorConfig.EventListPollIntervalSeconds * MillisPerSecond)
            }
        }
    }

    /** Immediate out-of-band fetch — does not wait for the next scheduled tick. */
    suspend fun refreshNow() {
        fetch()
    }

    private suspend fun fetch() {
        _events.value = listUpcomingEvents.execute(city = lastCity, genre = lastGenre)
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
