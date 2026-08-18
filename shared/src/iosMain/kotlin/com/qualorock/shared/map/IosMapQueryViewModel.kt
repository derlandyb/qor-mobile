package com.qualorock.shared.map

import com.qualorock.shared.feed.Closeable
import com.qualorock.shared.filters.FilterViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Swift-friendly facade over [MapQueryViewModel] — Kotlin's [kotlinx.coroutines.flow.StateFlow] isn't
 * directly consumable from Swift. [filterViewModel] is the same lifted instance the Feed tab uses (see
 * [com.qualorock.shared.filters.IosSharedFilterViewModel]), not owned here.
 */
class IosMapQueryViewModel(baseUrl: String, filterViewModel: FilterViewModel) {
    private val lifecycleJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + lifecycleJob)

    private val repository = KtorMapRepository(baseUrl)
    private val mapQueryViewModel = MapQueryViewModel(repository, filterViewModel, scope)

    fun retry() = mapQueryViewModel.retry()

    fun watchMarkers(onState: (MapMarkersUiState) -> Unit): Closeable = watch(mapQueryViewModel.markersState, onState)

    private fun <T> watch(
        flow: kotlinx.coroutines.flow.StateFlow<T>,
        onState: (T) -> Unit,
    ): Closeable {
        val watchJob = Job(lifecycleJob)
        flow.onEach { onState(it) }.launchIn(CoroutineScope(Dispatchers.Main + watchJob))
        return object : Closeable {
            override fun close() {
                watchJob.cancel()
            }
        }
    }

    fun close() {
        repository.close()
        lifecycleJob.cancel()
    }
}
