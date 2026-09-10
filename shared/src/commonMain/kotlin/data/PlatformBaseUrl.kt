package data

/**
 * The local-development API host differs per platform's loopback convention: Android's emulator
 * reaches the host machine via the `10.0.2.2` alias, while iOS Simulator (and the `jvm()` test
 * target) share the host's own network stack and use `localhost` directly.
 */
expect val platformBaseUrl: String
