package data

/**
 * Port for the fan's Sanctum bearer token, backed by platform secure storage per ARCHITECTURE
 * §2 — Android `EncryptedSharedPreferences`, iOS Keychain. Never plain app storage
 * (`SharedPreferences`/`UserDefaults`).
 */
interface SecureTokenStorage {
    suspend fun save(token: String)
    suspend fun read(): String?
    suspend fun clear()
}

/** Platform factory — `expect`/`actual` per S8, real storage on Android/iOS, in-memory on jvm(). */
expect fun createSecureTokenStorage(): SecureTokenStorage
