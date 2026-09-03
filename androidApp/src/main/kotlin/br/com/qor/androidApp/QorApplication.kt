package br.com.qor.androidApp

import android.app.Application
import br.com.qor.androidApp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** A1 — Koin DI bootstrap, so every screen resolves `shared`'s repositories/use cases via [appModule]. */
class QorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QorApplication)
            modules(appModule)
        }
    }
}
