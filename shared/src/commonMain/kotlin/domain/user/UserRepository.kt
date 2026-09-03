package domain.user

import domain.enum.ConsentType

/** Result of a registration attempt — pt-BR server messages pass through unmodified. */
sealed class RegisterResult {
    data class Success(val user: User) : RegisterResult()
    data class Failure(val message: String, val fieldErrors: Map<String, List<String>> = emptyMap()) : RegisterResult()
}

/** Result of a login attempt — see `api.md` T27 for the distinct branches this models. */
sealed class LoginResult {
    data class Success(val user: User, val token: String) : LoginResult()

    /** Wrong password/unknown email — a single generic message, never "which one was wrong". */
    data class InvalidCredentials(val message: String) : LoginResult()

    /** Account exists but its email isn't verified yet — offers a resend, per AUTH-10. */
    data class UnverifiedAccount(val message: String) : LoginResult()
}

/** Result of confirming a password-reset token, per `api.md` T28. */
sealed class ConfirmResetResult {
    data object Success : ConfirmResetResult()
    data class Failure(val message: String) : ConfirmResetResult()
}

/** Fields a fan can edit via `UpdateProfile` (T29) — omitted fields are left unchanged. */
data class ProfileUpdateFields(
    val name: String? = null,
    val phone: String? = null,
    val profilePictureUrl: String? = null,
    val email: String? = null,
)

/** Result of an `ExerciseDataRight` call, per `api.md` T30. */
sealed class DataRightResult {
    data class AccessGranted(val summary: String) : DataRightResult()
    data class ExportReady(val payload: String) : DataRightResult()
    data object DeletionConfirmed : DataRightResult()
    data object ConsentRevoked : DataRightResult()
    data class Failure(val message: String) : DataRightResult()
}

/** Zero-framework-dependency repository port for fan auth + profile management. */
interface UserRepository {
    suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult

    suspend fun login(email: String, password: String): LoginResult

    suspend fun loginWithGoogle(googleIdToken: String): LoginResult

    suspend fun logout()

    /** Step 1 of the 2-step reset flow — no account enumeration (`api.md` T28). */
    suspend fun requestPasswordReset(email: String)

    /** Step 2 — confirms with the emailed token. */
    suspend fun confirmPasswordReset(token: String, newPassword: String): ConfirmResetResult

    suspend fun getProfile(): User

    suspend fun updateProfile(fields: ProfileUpdateFields): User

    suspend fun accessData(): DataRightResult
    suspend fun exportData(): DataRightResult
    suspend fun deleteAccount(): DataRightResult
    suspend fun revokeConsent(consentType: ConsentType): DataRightResult
}
