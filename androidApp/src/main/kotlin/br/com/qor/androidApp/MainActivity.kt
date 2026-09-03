package br.com.qor.androidApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

/**
 * Bare entry point for the KMP project graph to be valid. No product UI lives here yet —
 * screens are built in the Android UI tasks (A1-A14), out of scope for the Shared module
 * foundation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    Text("QOR")
                }
            }
        }
    }
}
