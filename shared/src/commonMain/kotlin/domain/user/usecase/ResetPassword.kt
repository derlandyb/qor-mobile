package domain.user.usecase

import domain.user.ConfirmResetResult
import domain.user.UserRepository
import domain.user.VerifyResetCodeResult

/** Thin wrapper over [UserRepository]'s 3-step reset flow, per `api.md` T28. */
class ResetPassword(private val userRepository: UserRepository) {
    suspend fun requestReset(email: String) = userRepository.requestPasswordReset(email)

    suspend fun verifyResetCode(email: String, code: String): VerifyResetCodeResult =
        userRepository.verifyResetCode(email, code)

    suspend fun confirmReset(email: String, token: String, newPassword: String): ConfirmResetResult =
        userRepository.confirmPasswordReset(email, token, newPassword)
}
