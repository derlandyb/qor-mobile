package com.qualorock.shared.detail

import com.qualorock.shared.data.KtorEventRepository
import com.qualorock.shared.feed.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Swift-friendly facade over [EventDetailViewModel] — Kotlin's [kotlinx.coroutines.flow.StateFlow]
 * isn't directly consumable from Swift.
 */
class IosEventDetailViewModel(eventId: String, baseUrl: String) {
    private val lifecycleJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + lifecycleJob)
    private val viewModel = EventDetailViewModel(KtorEventRepository(baseUrl), eventId, scope)

    fun watch(onState: (EventDetailUiState) -> Unit): Closeable {
        val watchJob = Job(lifecycleJob)
        viewModel.state
            .onEach { onState(it) }
            .launchIn(CoroutineScope(Dispatchers.Main + watchJob))
        return object : Closeable {
            override fun close() {
                watchJob.cancel()
            }
        }
    }

    fun load() = viewModel.load()

    fun retry() = viewModel.retry()

    fun close() = lifecycleJob.cancel()
}
