package data

import co.touchlab.kermit.Logger as KermitLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Request timeout — named constant, never an inline literal at a call site (ARCHITECTURE §14). */
private const val RequestTimeoutMs = 15_000L

private val qorJson = Json {
    ignoreUnknownKeys = true
    isLenient = false
    prettyPrint = true
}

/**
 * Builds the shared Ktor [HttpClient] used by every repository — content negotiation and
 * timeouts are platform-independent and configured once here (`commonMain`), per S6's split
 * between platform engine selection ([createHttpClientEngine]) and shared client config.
 */
fun createQorHttpClient(engine: HttpClientEngine = createHttpClientEngine(), isDebugEnvironment: Boolean): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(qorJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = RequestTimeoutMs
            connectTimeoutMillis = RequestTimeoutMs
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    KermitLogger.d { message }
                }
            }

            // BODY (not ALL): ALL also logs headers, which would put the bearer token
            // (ARCHITECTURE §2) into Logcat/console on every debug build, including ones
            // handed to QA/testers whose device logs can end up in bug reports.
            level = if (isDebugEnvironment) {
                LogLevel.BODY
            } else {
                LogLevel.NONE
            }
        }
    }
