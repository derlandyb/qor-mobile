import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.startup.runtime)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
            }
        }
    }
}

android {
    namespace = "br.com.qor.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

detekt {
    buildUponDefaultConfig = true
}

/**
 * A1 (mobile.md) — closes a pre-existing gap: this module applied `kover` since S1 scaffolding
 * but never configured a coverage threshold, so `mobile` had no enforced 80%-coverage CI gate
 * at all (ARCHITECTURE §8.3). `total` aggregates coverage across every KMP target's tested
 * sources (commonTest run on jvm()) into the one number `./gradlew koverVerify` checks.
 *
 * Excludes are the Android/iOS `actual` platform-secure-storage/HTTP-engine implementations and
 * the app-context/startup glue around them — S8's own docblocks already scope these as
 * "Gate: build-verified only... needs an Android runtime", since `commonTest` runs on the jvm()
 * target and can't instantiate `EncryptedSharedPreferences`/App Startup. `InMemorySecureTokenStorage`
 * (jvmMain) is the same category — dev/test scaffolding, not exercised via a unit test.
 */
kover {
    reports {
        filters {
            excludes {
                classes(
                    "data.AndroidAppContext*",
                    "data.AndroidSecureTokenStorage*",
                    "data.InMemorySecureTokenStorage",
                    "data.HttpClientEngineFactory_*Kt",
                    "data.SecureTokenStorage_*Kt",
                )
            }
        }
        total {
            verify {
                rule {
                    minBound(80)
                }
            }
        }
    }
}

/**
 * S4 - a Clean Architecture boundary check (ARCHITECTURE section 8.5), scoped ONLY to the
 * domain package tree: forbids Android/iOS/native-interop imports leaking into the domain
 * layer. Kept as a separate task (not the module-wide `detekt` task) because `data`,
 * `androidMain`, and `iosMain` legitimately import those platform packages.
 */
val detektDomainBoundary = tasks.register("detektDomainBoundary", Detekt::class) {
    description = "Fails if shared's domain package imports Android/iOS/native framework code."
    group = "verification"
    setSource(files("src/commonMain/kotlin/domain"))
    config.setFrom(files("$rootDir/config/detekt/domain-boundary.yml"))
    buildUponDefaultConfig = false
    include("**/*.kt")
}

tasks.named("check") {
    dependsOn(detektDomainBoundary)
}

// Wired into S2's `./gradlew detekt` CI step, per S4's "Done when".
tasks.matching { it.name == "detekt" }.configureEach {
    dependsOn(detektDomainBoundary)
}
