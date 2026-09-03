package data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders

/**
 * Attaches the fan's bearer token (read from [SecureTokenStorage]) to every outgoing request,
 * per S8's "Attach the bearer token via a Ktor request-interceptor plugin" instruction.
 */
private val BearerTokenPlugin = createClientPlugin("BearerTokenPlugin", ::BearerTokenPluginConfig) {
    val tokenStorage = pluginConfig.tokenStorage
    onRequest { request, _ ->
        tokenStorage.read()?.let { token ->
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}

private class BearerTokenPluginConfig {
    lateinit var tokenStorage: SecureTokenStorage
}

/** Wraps [client] with [BearerTokenPlugin] so authenticated repositories never build headers manually. */
fun createAuthenticatedHttpClient(client: HttpClient, tokenStorage: SecureTokenStorage): HttpClient =
    client.config {
        install(BearerTokenPlugin) {
            this.tokenStorage = tokenStorage
        }
    }
