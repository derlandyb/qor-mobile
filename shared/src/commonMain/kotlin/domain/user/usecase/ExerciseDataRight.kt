package domain.user.usecase

import domain.enum.ConsentType
import domain.user.DataRightResult
import domain.user.UserRepository

/** Thin wrapper over [UserRepository]'s LGPD data-rights calls, per `api.md` T30. */
class ExerciseDataRight(private val userRepository: UserRepository) {
    suspend fun access(): DataRightResult = userRepository.accessData()
    suspend fun export(): DataRightResult = userRepository.exportData()
    suspend fun delete(): DataRightResult = userRepository.deleteAccount()
    suspend fun revokeConsent(consentType: ConsentType): DataRightResult = userRepository.revokeConsent(consentType)
}
