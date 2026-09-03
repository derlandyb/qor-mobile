package data

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

/**
 * `jvm()` target only runs `commonTest` (S1) — this engine is never shipped, it exists purely
 * so the jvm target compiles `commonMain`'s `expect` declaration.
 */
actual fun createHttpClientEngine(): HttpClientEngine = CIO.create()
