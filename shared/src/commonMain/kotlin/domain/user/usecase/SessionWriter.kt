package domain.user.usecase

import domain.user.User

/**
 * Port [AuthenticateFan] depends on to persist a successful login, implemented by S10's
 * `SessionStore` — kept here (not a forward dependency on S10) so S9 has no dependency on S10.
 */
interface SessionWriter {
    suspend fun onAuthenticated(user: User, token: String)
}
