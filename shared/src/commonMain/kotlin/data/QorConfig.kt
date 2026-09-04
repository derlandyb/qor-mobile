package data

/**
 * Client-side mirror of `qor-api`'s `config('qor.polling.event_list_interval_seconds')`
 * (ARCHITECTURE §14.2/§14.3) — a named constant so no call site inlines the interval literal.
 */
object QorConfig {
    const val EventListPollIntervalSeconds: Long = 30

    /**
     * Mirrors `qor-website`'s `OtpCodeInput` default `resendCooldownSeconds` (AUTH-10) — kept
     * here rather than inlined in `androidApp` so both clients read the same named constant.
     */
    const val EmailVerificationResendCooldownSeconds: Long = 60
}
