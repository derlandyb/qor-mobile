package data

import domain.user.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("email_verified_at") val emailVerifiedAt: String? = null,
    val phone: String? = null,
    @SerialName("profile_picture_url") val profilePictureUrl: String? = null,
    val birthdate: String,
)

@Serializable
internal data class UserResponseDto(val data: UserDto)

@Serializable
internal data class SessionResponseDto(
    val data: UserDto,
    val token: String,
)

@Serializable
internal data class VerifyEmailResponseDto(val data: UserDto? = null)

@Serializable
internal data class ErrorResponseDto(
    val message: String,
    val errors: Map<String, List<String>> = emptyMap(),
)

internal fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    emailVerifiedAt = emailVerifiedAt,
    phone = phone,
    profilePictureUrl = profilePictureUrl,
    birthdate = birthdate,
)
