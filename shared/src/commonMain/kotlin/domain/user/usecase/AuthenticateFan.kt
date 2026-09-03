package domain.user.usecase

import domain.user.LoginResult
import domain.user.UserRepository

/**
 * Thin wrapper over [UserRepository.login]/[UserRepository.loginWithGoogle], per `api.md`
 * T27's client-facing contract. On success, notifies [sessionWriter] so the shared session
 * store (S10) persists the token without S9 depending on S10 directly.
 */
class AuthenticateFan(
    private val userRepository: UserRepository,
    private val sessionWriter: SessionWriter,
) {
    suspend fun executeWithPassword(email: String, password: String): LoginResult {
        val result = userRepository.login(email, password)
        notifyIfSuccess(result)
        return result
    }

    suspend fun executeWithGoogle(googleIdToken: String): LoginResult {
        val result = userRepository.loginWithGoogle(googleIdToken)
        notifyIfSuccess(result)
        return result
    }

    private suspend fun notifyIfSuccess(result: LoginResult) {
        if (result is LoginResult.Success) {
            sessionWriter.onAuthenticated(result.user, result.token)
        }
    }
}
