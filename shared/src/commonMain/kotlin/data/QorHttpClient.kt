package data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Request timeout — named constant, never an inline literal at a call site (ARCHITECTURE §14). */
private const val RequestTimeoutMs = 15_000L

private val qorJson = Json {
    ignoreUnknownKeys = true
    isLenient = false
}

/**
 * Builds the shared Ktor [HttpClient] used by every repository — content negotiation and
 * timeouts are platform-independent and configured once here (`commonMain`), per S6's split
 * between platform engine selection ([createHttpClientEngine]) and shared client config.
 */
fun createQorHttpClient(engine: HttpClientEngine = createHttpClientEngine()): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(qorJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = RequestTimeoutMs
            connectTimeoutMillis = RequestTimeoutMs
        }
    }
