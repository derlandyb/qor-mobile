package data

/** Fake [SecureTokenStorage] for unit tests — the real `expect`/`actual` platform code cannot
 * be unit-tested on a bare JVM target, per S8's "Done when" note. */
class FakeSecureTokenStorage(initialToken: String? = null) : SecureTokenStorage {
    private var token: String? = initialToken

    override suspend fun save(token: String) {
        this.token = token
    }

    override suspend fun read(): String? = token

    override suspend fun clear() {
        token = null
    }
}
