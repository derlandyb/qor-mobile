package data

import domain.enum.ConsentType
import domain.user.ConfirmResetResult
import domain.user.DataRightResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import domain.user.User
import domain.user.UserRepository
import domain.user.VerifyEmailResult
import domain.user.VerifyResetCodeResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val lenientJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class RegisterRequestDto(
    val email: String,
    val password: String,
    val birthdate: String,
    val name: String,
    @SerialName("consent_accepted") val consentAccepted: Boolean,
)

@Serializable
private data class LoginRequestDto(val email: String, val password: String)

@Serializable
private data class GoogleLoginRequestDto(@SerialName("id_token") val idToken: String)

@Serializable
private data class RequestResetRequestDto(val email: String)

@Serializable
private data class VerifyResetCodeRequestDto(val email: String, val code: String)

@Serializable
private data class ConfirmResetRequestDto(
    val email: String,
    val token: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
private data class ResendVerificationRequestDto(val email: String)

@Serializable
private data class VerifyEmailCodeRequestDto(val email: String, val code: String)

@Serializable
private data class UpdateProfileRequestDto(
    val name: String? = null,
    val phone: String? = null,
    @SerialName("profile_picture_url") val profilePictureUrl: String? = null,
    val email: String? = null,
)

/** Ktor-based [UserRepository], calling `api.md` T31 (auth) / T32 (profile) endpoints. */
class UserRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String = ApiConfig.BaseUrl,
) : UserRepository {

    private val authBase get() = "$baseUrl${ApiConfig.ApiV1Prefix}/auth"
    private val profileBase get() = "$baseUrl${ApiConfig.ApiV1Prefix}/profile"

    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult {
        val response = httpClient.post("$authBase/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(email, password, birthdate, name, consentAccepted))
        }
        return if (response.status.isSuccess()) {
            RegisterResult.Success(response.body<UserResponseDto>().data.toDomain())
        } else {
            val error = response.body<ErrorResponseDto>()
            RegisterResult.Failure(error.message, error.errors)
        }
    }

    override suspend fun login(email: String, password: String): LoginResult {
        val response = httpClient.post("$authBase/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email, password))
        }
        return mapLoginResponse(response)
    }

    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult {
        val response = httpClient.post("$authBase/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleLoginRequestDto(googleIdToken))
        }
        return mapLoginResponse(response)
    }

    private suspend fun mapLoginResponse(response: HttpResponse): LoginResult {
        if (response.status.isSuccess()) {
            val session = response.body<SessionResponseDto>()
            return LoginResult.Success(session.data.toDomain(), session.token)
        }
        val error = response.body<ErrorResponseDto>()
        return if (response.status == HttpStatusCode.Forbidden) {
            LoginResult.UnverifiedAccount(error.message)
        } else {
            LoginResult.InvalidCredentials(error.message)
        }
    }

    override suspend fun logout() {
        httpClient.post("$authBase/logout")
    }

    override suspend fun requestPasswordReset(email: String) {
        httpClient.post("$authBase/password/forgot") {
            contentType(ContentType.Application.Json)
            setBody(RequestResetRequestDto(email))
        }
    }

    override suspend fun verifyResetCode(email: String, code: String): VerifyResetCodeResult {
        val response = httpClient.post("$authBase/password/verify-code") {
            contentType(ContentType.Application.Json)
            setBody(VerifyResetCodeRequestDto(email, code))
        }
        return if (response.status.isSuccess()) {
            VerifyResetCodeResult.Success(response.body<VerifyResetCodeResponseDto>().data.token)
        } else {
            VerifyResetCodeResult.Failure(response.body<ErrorResponseDto>().message)
        }
    }

    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult {
        val response = httpClient.post("$authBase/password/reset") {
            contentType(ContentType.Application.Json)
            setBody(ConfirmResetRequestDto(email, token, newPassword))
        }
        return if (response.status.isSuccess()) {
            ConfirmResetResult.Success
        } else {
            ConfirmResetResult.Failure(response.body<ErrorResponseDto>().message)
        }
    }

    override suspend fun resendVerification(email: String) {
        httpClient.post("$authBase/email/verification-notification") {
            contentType(ContentType.Application.Json)
            setBody(ResendVerificationRequestDto(email))
        }
    }

    override suspend fun verifyEmailCode(email: String, code: String): VerifyEmailResult {
        val response = httpClient.post("$authBase/email/verify-code") {
            contentType(ContentType.Application.Json)
            setBody(VerifyEmailCodeRequestDto(email, code))
        }
        return if (response.status.isSuccess()) {
            VerifyEmailResult.Success(response.body<VerifyEmailResponseDto>().data?.toDomain())
        } else {
            VerifyEmailResult.Failure(response.body<ErrorResponseDto>().message)
        }
    }

    override suspend fun getProfile(): User =
        httpClient.get(profileBase).body<UserResponseDto>().data.toDomain()

    override suspend fun updateProfile(fields: ProfileUpdateFields): User {
        val response = httpClient.patch(profileBase) {
            contentType(ContentType.Application.Json)
            setBody(
                UpdateProfileRequestDto(
                    name = fields.name,
                    phone = fields.phone,
                    profilePictureUrl = fields.profilePictureUrl,
                    email = fields.email,
                ),
            )
        }
        return response.body<UserResponseDto>().data.toDomain()
    }

    override suspend fun accessData(): DataRightResult = dataRightCall("$profileBase/data-rights/access") { body ->
        DataRightResult.AccessGranted(body)
    }

    override suspend fun exportData(): DataRightResult = dataRightCall("$profileBase/data-rights/export") { body ->
        DataRightResult.ExportReady(body)
    }

    override suspend fun deleteAccount(): DataRightResult = dataRightCall("$profileBase/data-rights/delete") {
        DataRightResult.DeletionConfirmed
    }

    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult =
        dataRightCall("$profileBase/data-rights/revoke") { DataRightResult.ConsentRevoked }

    private suspend fun dataRightCall(
        url: String,
        onSuccess: (String) -> DataRightResult,
    ): DataRightResult {
        val response = httpClient.post(url)
        val rawBody = response.bodyAsText()
        return if (response.status.isSuccess()) {
            onSuccess(rawBody)
        } else {
            val error = runCatching { lenientJson.decodeFromString(ErrorResponseDto.serializer(), rawBody) }
                .getOrDefault(ErrorResponseDto(message = rawBody))
            DataRightResult.Failure(error.message)
        }
    }
}
