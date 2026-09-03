package data

/**
 * Client-side mirror of `qor-api`'s `config('qor.polling.event_list_interval_seconds')`
 * (ARCHITECTURE §14.2/§14.3) — a named constant so no call site inlines the interval literal.
 */
object QorConfig {
    const val EventListPollIntervalSeconds: Long = 30
}
