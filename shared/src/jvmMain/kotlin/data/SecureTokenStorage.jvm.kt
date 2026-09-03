package data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * `jvm()` target only runs `commonTest` (S1) — never shipped. Unit tests inject a fake
 * [SecureTokenStorage] instead of this one (S8's "Done when"); this exists purely so the jvm
 * target compiles `commonMain`'s `expect` declaration.
 */
private class InMemorySecureTokenStorage : SecureTokenStorage {
    private val mutex = Mutex()
    private var token: String? = null

    override suspend fun save(token: String) {
        mutex.withLock { this.token = token }
    }

    override suspend fun read(): String? = mutex.withLock { token }

    override suspend fun clear() {
        mutex.withLock { token = null }
    }
}

actual fun createSecureTokenStorage(): SecureTokenStorage = InMemorySecureTokenStorage()
