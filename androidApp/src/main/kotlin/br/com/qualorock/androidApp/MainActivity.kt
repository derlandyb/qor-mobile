package br.com.qualorock.androidApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val HomeRoute = "home"

/**
 * A1 — app entry point wired for Compose Navigation. The real `NavHost` graph (every MVP Core
 * screen, `BottomNav` integration) is A14's job; this single placeholder route only proves the
 * `androidx.navigation:navigation-compose` wiring compiles and runs.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = HomeRoute) {
                        composable(HomeRoute) {
                            Text("QOR")
                        }
                    }
                }
            }
        }
    }
}
