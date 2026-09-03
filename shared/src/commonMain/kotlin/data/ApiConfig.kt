package data

/**
 * Central network configuration — named constants so no controller/repository ever inlines a
 * base URL literal (ARCHITECTURE §14.3's `API_BASE_URL` convention for client repos).
 *
 * `BaseUrl` is a build-time default for local development against `qor-api`'s Docker Compose
 * stack; a release build overrides it via build-config/environment injection, not by editing
 * this file per environment.
 */
object ApiConfig {
    const val BaseUrl: String = "http://localhost:8000"
    const val ApiV1Prefix: String = "/api/v1"
}
