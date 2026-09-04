package br.com.qualorock.androidApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import br.com.qualorock.androidApp.ui.nav.QorNavGraph
import br.com.qualorock.androidApp.ui.theme.QorTheme
import data.SessionStore
import org.koin.compose.koinInject

/**
 * A1/A14 — app entry point. The real `NavHost` graph (every MVP Core screen, `BottomNav`
 * integration, startup session restore) lives in [QorNavGraph]; this Activity only wires the
 * themed root and hands the graph its [SessionStore] via Koin.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QorTheme {
                Surface {
                    val sessionStore = koinInject<SessionStore>()
                    QorNavGraph(sessionStore = sessionStore)
                }
            }
        }
    }
}
