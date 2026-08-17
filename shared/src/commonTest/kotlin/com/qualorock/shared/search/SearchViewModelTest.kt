package com.qualorock.shared.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test
    fun `given rapid keystrokes when debounceMs elapses once then only the final query is emitted`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val viewModel = SearchViewModel(scope, minQueryLength = 2, debounceMs = 300)

            viewModel.query.value = "fo"
            advanceTimeBy(100)
            viewModel.query.value = "for"
            advanceTimeBy(100)
            viewModel.query.value = "forro"
            advanceUntilIdle()

            assertEquals("forro", viewModel.debouncedQuery.value)
        }

    @Test
    fun `given a query below minQueryLength when debounceMs elapses then debouncedQuery is null`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val viewModel = SearchViewModel(scope, minQueryLength = 2, debounceMs = 300)

            viewModel.query.value = "f"
            advanceUntilIdle()

            assertNull(viewModel.debouncedQuery.value)
        }

    @Test
    fun `given an empty or whitespace-only query when debounceMs elapses then debouncedQuery is null`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val viewModel = SearchViewModel(scope, minQueryLength = 2, debounceMs = 300)

            viewModel.query.value = "   "
            advanceUntilIdle()

            assertNull(viewModel.debouncedQuery.value)
        }
}
