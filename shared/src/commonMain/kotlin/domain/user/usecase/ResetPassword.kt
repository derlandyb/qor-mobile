package domain.user.usecase

import domain.user.ConfirmResetResult
import domain.user.UserRepository

/** Thin wrapper over [UserRepository]'s 2-step reset flow, per `api.md` T28. */
class ResetPassword(private val userRepository: UserRepository) {
    suspend fun requestReset(email: String) = userRepository.requestPasswordReset(email)

    suspend fun confirmReset(token: String, newPassword: String): ConfirmResetResult =
        userRepository.confirmPasswordReset(token, newPassword)
}
