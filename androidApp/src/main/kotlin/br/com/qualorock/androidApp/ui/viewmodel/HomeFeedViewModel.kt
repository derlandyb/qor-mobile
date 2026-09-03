package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.enum.City
import domain.event.Event
import domain.event.EventPage
import domain.event.PollingCoordinator
import domain.event.usecase.ListUpcomingEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** UI state for [HomeFeedScreen][br.com.qualorock.androidApp.ui.screen.HomeFeedScreen] (A11, DISC-01–DISC-06). */
sealed class HomeFeedUiState {
    data object Loading : HomeFeedUiState()

    /**
     * [isLoadingMore] drives the trailing spinner for a pagination fetch in flight;
     * [isRefreshing] drives the pull-to-refresh indicator for a manual [HomeFeedViewModel.onRefresh]
     * in flight. Both default `false` so [HomeFeedUiState.Content] stays a minimal happy-path
     * constructor for the common case.
     */
    data class Content(
        val events: List<Event>,
        val isLoadingMore: Boolean = false,
        val isRefreshing: Boolean = false,
    ) : HomeFeedUiState()

    data object Empty : HomeFeedUiState()

    /**
     * `EventRepository`/`ListUpcomingEvents` carry no server-message contract to show verbatim
     * (unlike `UserRepository`'s result types, AUTH-11-style) — just one generic pt-BR failure
     * case, so no message payload here. [HomeFeedScreen][br.com.qualorock.androidApp.ui.screen.HomeFeedScreen]
     * resolves the copy from `R.string.home_feed_error_message`, keeping this ViewModel
     * Android-resource-agnostic like [LoginViewModel]/[SignupViewModel]'s field-error enums.
     */
    data object Error : HomeFeedUiState()
}

/**
 * A11 — public event feed (DISC-01–DISC-06), no login required. The initial load and [onLoadMore]
 * pagination call [ListUpcomingEvents] directly (both need per-call error visibility a `sealed`
 * state can show); [PollingCoordinator] drives the shared 30s live-refresh loop (`QorConfig`'s
 * `EventListPollIntervalSeconds`, S11) and backs [onRefresh]'s manual pull-to-refresh trigger.
 *
 * **Why the initial load calls [ListUpcomingEvents.execute] directly instead of
 * [PollingCoordinator.start]/[PollingCoordinator.refreshNow]:** [PollingCoordinator]'s poll loop
 * (`while (true) { fetch(); delay(...) }`, kicked off by [start]) never surfaces a fetch failure
 * to any caller — an exception there just silently kills that loop's `Job`, with no way for a UI
 * layer to show [HomeFeedUiState.Error]. A direct, `try`/`catch`-wrapped call is the only way this
 * screen can show an error for its first load. The cost: [start] still performs its own immediate
 * first fetch right after, so cold-starting this screen fires two page-1 fetches back-to-back (our
 * direct call plus [PollingCoordinator]'s own internal first tick) — an unavoidable duplicate
 * given [PollingCoordinator] has no "start without an immediate fetch" option; not something this
 * task's scope extends to changing in `shared`. [onRefresh] uses [PollingCoordinator.refreshNow]
 * instead (a plain suspend call whose exception *does* propagate to its caller, unlike the poll
 * loop), matching its own KDoc ("immediate out-of-band fetch") — the idiomatic use of that API.
 *
 * **Why [PollingCoordinator.events] is collected with [drop] of its first emission:** a
 * [kotlinx.coroutines.flow.StateFlow] replays its *current* value to a new collector
 * synchronously, before any new fetch resolves — here that's always either the coordinator's
 * still-unfetched default (`EventPage(emptyList(), null)`) or a stale value left over from a
 * previous screen visit (the coordinator is a Koin singleton). Applying either blindly could wrongly
 * flip a just-set [HomeFeedUiState.Error] to [HomeFeedUiState.Empty], or show stale data before a
 * fresh fetch resolves. Dropping it means only genuinely new pushes — a real periodic tick, or a
 * real [PollingCoordinator.refreshNow] call — ever update state through this collector.
 *
 * **Pagination and polling both replace/append against the same page-1 concept, but
 * [PollingCoordinator] only ever knows page 1** (it calls [ListUpcomingEvents.execute] with no
 * `cursor`). So a periodic tick (or a pull-to-refresh) landing while extra pages are loaded via
 * [onLoadMore] resets the visible list back to that fresh page 1, discarding previously appended
 * pages — an accepted consequence of the shared coordinator's page-1-only contract, not a bug to
 * work around client-side.
 *
 * **[start] is given an isolated [pollingScope], not [viewModelScope] directly.**
 * [PollingCoordinator]'s poll loop has no internal `try`/`catch` around its `fetch()` call — a
 * failure there is an *uncaught* exception in a fire-and-forget `launch`, which (with no
 * [CoroutineExceptionHandler] in the picture) propagates to the thread's default handler instead
 * of quietly dying. [pollingScope] installs one that swallows it, so a background tick failing
 * can never crash the app; the tradeoff already documented above is that such a failure is then
 * invisible (no [HomeFeedUiState.Error]), same as calling [PollingCoordinator.start] always was.
 *
 * **`open`, and [currentCity]/[currentGenre]/[applyFilters] are `protected`, for A12's
 * [br.com.qualorock.androidApp.ui.viewmodel.ExploreViewModel] to subclass.** DISC-14–DISC-18 need
 * the exact same pagination/polling/empty-state machinery this class already has, just parameterized
 * by a city/genre pair instead of always `null`/`null` — subclassing (smallest change available,
 * per A12's brief) reuses every private helper below (`loadInitial`, `onLoadMore`, `applyPage`,
 * `pollingScope`) unchanged; only [currentCity]/[currentGenre] fields were added plus the
 * [applyFilters] entry point that reruns [loadInitial] and restarts [pollingCoordinator] with the
 * new pair. This class's own two-arg constructor, public API, and behavior (both fields default
 * `null`) are unchanged, so A11's existing tests/callers need no changes.
 *
 * See also this class's own [HomeFeedUiState] doc block above; the [uiState] flow it powers is
 * reused as-is by [br.com.qualorock.androidApp.ui.viewmodel.ExploreViewModel] rather than being
 * duplicated behind a second, structurally-identical sealed type.
 */
open class HomeFeedViewModel(
    private val listUpcomingEvents: ListUpcomingEvents,
    private val pollingCoordinator: PollingCoordinator,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeFeedUiState>(HomeFeedUiState.Loading)
    val uiState: StateFlow<HomeFeedUiState> = _uiState.asStateFlow()

    private var nextCursor: String? = null

    /** A12 — current filters; always `null`/`null` for this class's own [HomeFeedScreen] usage. */
    protected var currentCity: City? = null
        private set
    protected var currentGenre: String? = null
        private set

    /** See this class's KDoc — isolates [PollingCoordinator.start]'s uncaught internal failures from crashing [viewModelScope]. */
    private val pollingScope = CoroutineScope(
        viewModelScope.coroutineContext +
            SupervisorJob(viewModelScope.coroutineContext[Job]) +
            CoroutineExceptionHandler { _, _ -> },
    )

    private val observePollingJob: Job = viewModelScope.launch {
        loadInitial()
        observePolling()
    }

    init {
        pollingCoordinator.start(pollingScope, currentCity, currentGenre)
    }

    /**
     * A12/DISC-14–DISC-17 — changes the active filters and reruns both the direct page-1 load and
     * [PollingCoordinator]'s loop against the new pair, same as this class's own cold start. Only
     * [br.com.qualorock.androidApp.ui.viewmodel.ExploreViewModel] calls this; [HomeFeedScreen]'s
     * own usage of this class never does, so its behavior is unaffected.
     */
    protected fun applyFilters(city: City?, genre: String?) {
        currentCity = city
        currentGenre = genre
        nextCursor = null
        viewModelScope.launch { loadInitial() }
        pollingCoordinator.start(pollingScope, city, genre)
    }

    /** DISC-02 pagination — call when the list scrolls near its end. No-op with no next page or a fetch already in flight. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun onLoadMore() {
        val state = _uiState.value
        if (state !is HomeFeedUiState.Content || state.isLoadingMore || nextCursor == null) return

        _uiState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            try {
                val page = listUpcomingEvents.execute(city = currentCity, genre = currentGenre, cursor = nextCursor)
                nextCursor = page.nextCursor
                val merged = (_uiState.value as? HomeFeedUiState.Content)?.events.orEmpty() + page.events
                _uiState.value = HomeFeedUiState.Content(events = merged, isLoadingMore = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Network-boundary catch-all — `ListUpcomingEvents`/`EventRepository` carry no
                // typed failure contract (unlike `UserRepository`'s sealed results), and there's
                // no logging infra in this codebase yet to record [e] beyond falling back the UI.
                // Pagination fails silently here (spinner just stops) rather than surfacing a
                // dedicated error state — DISC-02 doesn't call for one, unlike the initial load.
                _uiState.value = state.copy(isLoadingMore = false)
            }
        }
    }

    /** DISC-04's manual pull-to-refresh trigger — an immediate out-of-band fetch via [PollingCoordinator.refreshNow]. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun onRefresh() {
        val current = _uiState.value
        if (current is HomeFeedUiState.Content) {
            _uiState.value = current.copy(isRefreshing = true)
        }
        viewModelScope.launch {
            try {
                pollingCoordinator.refreshNow()
                applyPage(pollingCoordinator.events.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val stateBeforeFailure = _uiState.value
                _uiState.value = if (stateBeforeFailure is HomeFeedUiState.Content) {
                    stateBeforeFailure.copy(isRefreshing = false)
                } else {
                    HomeFeedUiState.Error
                }
            }
        }
    }

    public override fun onCleared() {
        pollingCoordinator.stop()
        pollingScope.cancel()
        observePollingJob.cancel()
        super.onCleared()
    }

    /** See this class's KDoc for why the initial load bypasses [PollingCoordinator] entirely. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun loadInitial() {
        _uiState.value = HomeFeedUiState.Loading
        try {
            applyPage(listUpcomingEvents.execute(city = currentCity, genre = currentGenre))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.value = HomeFeedUiState.Error
        }
    }

    private suspend fun observePolling() {
        pollingCoordinator.events.drop(1).collect { page -> applyPage(page) }
    }

    private fun applyPage(page: EventPage) {
        nextCursor = page.nextCursor
        _uiState.value = if (page.events.isEmpty()) {
            HomeFeedUiState.Empty
        } else {
            HomeFeedUiState.Content(events = page.events)
        }
    }
}
