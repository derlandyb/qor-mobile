package com.qualorock.shared.feed

import com.qualorock.shared.data.KtorEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface Closeable {
    fun close()
}

/** Swift-friendly facade over [EventFeedViewModel] — Kotlin's [kotlinx.coroutines.flow.StateFlow] isn't directly consumable from Swift. */
class IosEventFeedViewModel(baseUrl: String) {
    private val lifecycleJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + lifecycleJob)
    private val viewModel = EventFeedViewModel(KtorEventRepository(baseUrl), scope)

    fun watch(onState: (EventFeedUiState) -> Unit): Closeable {
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

    fun loadNextPage() = viewModel.loadNextPage()

    fun retry() = viewModel.retry()
}
