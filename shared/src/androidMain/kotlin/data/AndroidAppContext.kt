package data

import android.content.Context
import androidx.startup.Initializer

/**
 * Holds the application [Context] without requiring `androidApp` to wire any DI/`Application`
 * subclass — populated automatically by [AndroidAppContextInitializer] via App Startup, which
 * `androidx.startup` runs before any app code, including before `MainActivity.onCreate`.
 */
internal object AndroidAppContext {
    lateinit var applicationContext: Context
        private set

    internal fun install(context: Context) {
        applicationContext = context.applicationContext
    }
}

/** Registered via App Startup's manifest `<meta-data>` merge — see the module's own manifest. */
class AndroidAppContextInitializer : Initializer<Context> {
    override fun create(context: Context): Context {
        AndroidAppContext.install(context)
        return context
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
