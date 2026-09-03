package domain.user.usecase

import domain.user.RegisterResult
import domain.user.UserRepository

/** Thin wrapper over [UserRepository.register], per `api.md` T26's client-facing contract. */
class RegisterFan(private val userRepository: UserRepository) {
    suspend fun execute(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = userRepository.register(email, password, birthdate, name, consentAccepted)
}
