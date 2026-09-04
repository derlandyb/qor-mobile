package di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * I1 — single Koin bootstrap entry point for platforms that have no platform-specific module to
 * add on top of [sharedModule] (iOS, for now — its ViewModels/screens are a later I-task).
 * Android keeps its own `startKoin` call in `QorApplication` since it also registers
 * `androidContext(...)` and the Android-only `viewModelModule`.
 *
 * Exposed as a plain no-default-argument function so it's callable directly from Swift via the
 * `shared` framework (`KoinHelperKt.doInitKoin()`) — Kotlin default-parameter values don't
 * survive Objective-C/Swift interop.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(sharedModule)
    }
}

/** Swift-callable entry point — see [initKoin]. */
fun doInitKoin() {
    initKoin()
}
