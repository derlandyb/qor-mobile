package domain.user.usecase

import domain.user.ProfileUpdateFields
import domain.user.User
import domain.user.UserRepository

/** Thin wrapper over [UserRepository.updateProfile], per `api.md` T29's client-facing contract. */
class UpdateProfile(private val userRepository: UserRepository) {
    suspend fun execute(fields: ProfileUpdateFields): User = userRepository.updateProfile(fields)
}
