package data

import domain.user.User
import domain.user.UserRepository
import domain.user.usecase.SessionWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared observable session state — logged-in [User] or `null` — consumable by both Compose
 * and SwiftUI via [currentUser] (AUTH-12: session persistence across app restarts). Implements
 * S9's [SessionWriter] port so [AuthenticateFan][domain.user.usecase.AuthenticateFan] can
 * notify it on successful login without depending on this class directly.
 */
class SessionStore(
    private val userRepository: UserRepository,
    private val tokenStorage: SecureTokenStorage,
) : SessionWriter {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /** Reads a stored token (if any) and fetches the profile to repopulate [currentUser]. */
    suspend fun restore() {
        val token = tokenStorage.read()
        _currentUser.value = if (token != null) userRepository.getProfile() else null
    }

    suspend fun set(user: User, token: String) {
        tokenStorage.save(token)
        _currentUser.value = user
    }

    suspend fun clear() {
        tokenStorage.clear()
        _currentUser.value = null
    }

    override suspend fun onAuthenticated(user: User, token: String) {
        set(user, token)
    }
}
