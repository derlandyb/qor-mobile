package domain.user.usecase

import domain.user.UserRepository
import domain.user.VerifyEmailResult

/**
 * Thin wrapper over [UserRepository]'s email-verification flow (AUTH-10) — resend the OTP
 * code, then confirm it. No [SessionWriter] involvement: `verifyEmailCode` doesn't issue a
 * session (see [VerifyEmailResult]), the fan logs in separately afterwards.
 */
class VerifyEmail(private val userRepository: UserRepository) {
    suspend fun resend(email: String) = userRepository.resendVerification(email)

    suspend fun verifyCode(email: String, code: String): VerifyEmailResult =
        userRepository.verifyEmailCode(email, code)
}
