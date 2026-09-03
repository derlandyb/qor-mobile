package data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

private const val KeychainService = "br.com.qualorock.mobile.auth"
private const val KeychainAccount = "qor_bearer_token"

/**
 * iOS bearer-token storage, hand-rolled against the Keychain Services C API (per S8's explicit
 * "not `multiplatform-settings`" instruction) per ARCHITECTURE §2 — the token never touches
 * `UserDefaults`. `Gate: build`-verified only, see S8's note (no bare-JVM unit test target for
 * this `actual`). String<->NSData conversion goes through a plain `ByteArray` (not
 * `NSString`), the standard, unambiguous Kotlin/Native interop pattern.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosKeychainSecureTokenStorage : SecureTokenStorage {

    override suspend fun save(token: String) {
        val data = token.toNsData()
        val query = queryDictionary()
        if (SecItemCopyMatching(query, null) == errSecSuccess) {
            val attributesToUpdate = mapOf<Any?, Any?>(kSecValueData to data)
            SecItemUpdate(query, CFBridgingRetain(attributesToUpdate) as CFDictionaryRef)
        } else {
            val attributes = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to KeychainService,
                kSecAttrAccount to KeychainAccount,
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock,
                kSecValueData to data,
            )
            SecItemAdd(CFBridgingRetain(attributes) as CFDictionaryRef, null)
        }
    }

    override suspend fun read(): String? = memScoped {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KeychainService,
            kSecAttrAccount to KeychainAccount,
            kSecReturnData to true,
            kSecMatchLimit to kSecMatchLimitOne,
        )
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(CFBridgingRetain(query) as CFDictionaryRef, result.ptr)
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        data.toKotlinString()
    }

    override suspend fun clear() {
        SecItemDelete(queryDictionary())
    }

    private fun queryDictionary(): CFDictionaryRef {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to KeychainService,
            kSecAttrAccount to KeychainAccount,
        )
        return CFBridgingRetain(query) as CFDictionaryRef
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun String.toNsData(): NSData {
    val bytes = encodeToByteArray()
    return bytes.usePinned {
        NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toKotlinString(): String {
    val byteArray = ByteArray(length.toInt())
    byteArray.usePinned {
        memcpy(it.addressOf(0), bytes, length)
    }
    return byteArray.decodeToString()
}

actual fun createSecureTokenStorage(): SecureTokenStorage = IosKeychainSecureTokenStorage()
