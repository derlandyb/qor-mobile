package domain.user.usecase

import domain.enum.ConsentType
import domain.user.ConfirmResetResult
import domain.user.DataRightResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import domain.user.User
import domain.user.UserRepository

class FakeUserRepository(
    private val loginResult: LoginResult = LoginResult.InvalidCredentials("nao configurado"),
    private val registerResult: RegisterResult = RegisterResult.Failure("nao configurado"),
    private val confirmResetResult: ConfirmResetResult = ConfirmResetResult.Failure("nao configurado"),
) : UserRepository {
    var requestedResetEmail: String? = null

    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = registerResult

    override suspend fun login(email: String, password: String): LoginResult = loginResult

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult = loginResult

    override suspend fun logout() = Unit

    override suspend fun requestPasswordReset(email: String) {
        requestedResetEmail = email
    }

    override suspend fun confirmPasswordReset(token: String, newPassword: String): ConfirmResetResult = confirmResetResult

    override suspend fun getProfile(): User = error("not configured")

    override suspend fun updateProfile(fields: ProfileUpdateFields): User = error("not configured")

    override suspend fun accessData(): DataRightResult = DataRightResult.AccessGranted("resumo")
    override suspend fun exportData(): DataRightResult = DataRightResult.ExportReady("payload")
    override suspend fun deleteAccount(): DataRightResult = DataRightResult.DeletionConfirmed
    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult = DataRightResult.ConsentRevoked
}
