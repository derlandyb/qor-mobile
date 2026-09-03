package data

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PrefsFileName = "qor_secure_token_prefs"
private const val TokenKey = "qor_bearer_token"

/**
 * Android bearer-token storage, backed by [EncryptedSharedPreferences] (AES-256-GCM value
 * encryption, keystore-backed master key) per ARCHITECTURE §2 — the token never touches plain
 * `SharedPreferences`. `Gate: build`-verified only (needs an Android runtime), see S8's note.
 */
private class AndroidSecureTokenStorage : SecureTokenStorage {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(AndroidAppContext.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            AndroidAppContext.applicationContext,
            PrefsFileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun save(token: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(TokenKey, token).apply()
    }

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        prefs.getString(TokenKey, null)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().remove(TokenKey).apply()
    }
}

actual fun createSecureTokenStorage(): SecureTokenStorage = AndroidSecureTokenStorage()
