package domain.user

/** Mirrors `qor-api`'s `User` (fan) entity shape (ARCHITECTURE §4), client-relevant fields. */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val emailVerifiedAt: String?,
    val phone: String?,
    val profilePictureUrl: String?,
    val birthdate: String,
)
