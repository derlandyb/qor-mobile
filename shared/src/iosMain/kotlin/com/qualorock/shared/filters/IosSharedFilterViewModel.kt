package com.qualorock.shared.filters

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

/**
 * Owns the single [FilterViewModel] instance (and its backing [CoroutineScope]) shared by the Feed and Map
 * tabs on iOS — constructed once in `iosAppApp.swift` and handed to both [IosFeedQueryViewModel] and
 * [com.qualorock.shared.map.IosMapQueryViewModel], so active filters survive switching tabs (MAP-003 AC2).
 */
class IosSharedFilterViewModel(baseUrl: String) {
    private val lifecycleJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + lifecycleJob)

    val filterViewModel = FilterViewModel(KtorFilterOptionsRepository(baseUrl), scope)

    fun close() = lifecycleJob.cancel()
}
