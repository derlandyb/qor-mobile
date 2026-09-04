package br.com.qualorock.androidApp

import android.app.Application
import br.com.qualorock.androidApp.di.viewModelModule
import di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * A1 — Koin DI bootstrap, so every screen resolves `shared`'s repositories/use cases via
 * [sharedModule] (I1 — the platform-agnostic bindings, shared with iOS) plus Android's own
 * [viewModelModule].
 */
class QorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QorApplication)
            modules(sharedModule, viewModelModule)
        }
    }
}
