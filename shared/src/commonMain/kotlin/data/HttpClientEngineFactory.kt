package data

import io.ktor.client.engine.HttpClientEngine

/**
 * Platform HTTP engine selection — the only part of the Ktor setup that differs per platform
 * (OkHttp on Android, Darwin on iOS, CIO on the `jvm()` test target). Client configuration
 * (content negotiation, timeouts) stays in [createQorHttpClient], `commonMain`, per S6's split.
 */
expect fun createHttpClientEngine(): HttpClientEngine
