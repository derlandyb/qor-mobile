import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
}

// A22 — this module's local-only API-key convention (none existed before A22). Read the same
// way Android projects conventionally read `sdk.dir` from `local.properties`: a gitignored,
// per-checkout file (verified via `git check-ignore`) rather than anything committed. Missing the
// file/property yields an empty string, not a build failure — `EventDetailScreen`'s `GoogleMap`
// handles a missing/invalid key at runtime via its own state (see `EventMapState.Failed`), it
// does not assume a real key is present in this environment.
val mapsApiKeyPropertyName = "MAPS_API_KEY"
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val mapsApiKey: String = localProperties.getProperty(mapsApiKeyPropertyName, "")

android {
    namespace = "br.com.qualorock.androidApp"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.qualorock.androidApp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders[mapsApiKeyPropertyName] = mapsApiKey
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// `ui-test-manifest` (needed by every Robolectric Compose UI test to launch a host activity) is
// `debugImplementation`-only, per AGP convention — it must never ship in a release build. The
// `release` unit-test variant can't run these tests as a result, so it's disabled outright
// rather than left to fail; `debug` is this app's only unit-tested variant either way.
tasks.matching { it.name == "testReleaseUnitTest" }.configureEach { enabled = false }

// ARCHITECTURE §8.4 — "Android Lint/detekt (Android + shared)" applies to this module too;
// it was only wired into `shared` at S1/S4 scaffolding time, leaving every A2+ Compose
// component/screen added here unchecked by static analysis. Uses the same default ruleset
// as `shared`'s module-wide `detekt` task (S4's `detektDomainBoundary` is a `shared`-only
// concern and does not apply here).
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$projectDir/config/detekt/detekt.yml"))
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation(libs.androidx.navigation.compose)
    // A22 — embedded event-detail map (`maps-compose`'s Compose wrapper over `play-services-maps`,
    // added explicitly since this module doesn't otherwise depend on Play Services).
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    // `shared` declares this as `implementation`, not `api` — needed here directly since
    // AppModule.kt (Koin) references `HttpClient` when wiring EventRepositoryImpl/UserRepositoryImpl.
    implementation(libs.ktor.client.core)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

/**
 * A1 — closes `mobile`'s pre-existing gap: `kover` was applied to `shared` (S1 scaffolding) but
 * never configured with a coverage threshold, and CI never ran a coverage-gated task at all
 * (ARCHITECTURE §8.3 mandates a minimum-80%-coverage CI gate per repo). This rule makes
 * `./gradlew koverVerifyDebug` fail the build below 80% line coverage on `androidApp`'s own
 * sources. Scoped to the `debug` variant only (not `total`/`release`) — `ui-test-manifest`
 * (Compose's empty host-activity manifest fragment, needed by every Robolectric Compose UI
 * test) is `debugImplementation`-only, the correct AGP convention since it must never ship in a
 * release build; the `release` unit-test variant can't run these tests at all as a result.
 */
kover {
    reports {
        // A1's own classes are framework bootstrap/declarative DI wiring, not business logic —
        // mobile.md itself scopes A1 to `Tests: none` / `Gate: build`, unlike every A2+ task
        // (`Tests: unit`, `Gate: quick`), so they're excluded from the 80% denominator rather
        // than padded with tests that would just assert Koin/Compose framework wiring.
        filters {
            excludes {
                classes(
                    "br.com.qualorock.androidApp.QorApplication*",
                    "br.com.qualorock.androidApp.*MainActivity*",
                    "br.com.qualorock.androidApp.di.*",
                )
            }
        }
        variant("debug") {
            verify {
                rule {
                    minBound(80)
                }
            }
        }
    }
}
