# Pinned tool versions

Per `.specs/project/ARCHITECTURE.md`'s Dev Environment section and `PLATFORM_BOOTSTRAP_TASKS.md` T1.2 — Android SDK, Kotlin, and Xcode are host-provided and must be pinned explicitly in this repository (they are not covered by the root `.mise.toml`, which only pins PHP/Node/Java for the backend/web stacks).

| Tool | Version |
|---|---|
| JDK | 17.0.11 (matches root `.mise.toml`) |
| Gradle | 8.9 (wrapper-pinned, see `gradle/wrapper/gradle-wrapper.properties`) |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 2.0.21 |
| Ktor | 2.3.12 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| Android build-tools | 34.0.0 |
| Xcode | 26.3 (Build 17C529) |
| iOS Simulator SDK | 26.2 |
| iOS deployment target | 16.0 |
| xcodegen | latest via Homebrew (generates `iosApp/iosApp.xcodeproj` from `iosApp/project.yml` — the `.xcodeproj` itself is gitignored) |

CI (`mobile/.github/workflows/ci.yml`) runs `format`/`lint`/`unit-tests` on `ubuntu-latest` (JDK/Gradle/Android SDK only) and `static-analysis`/`ui-e2e-tests`/`build`/`coverage` on `macos-latest` (adds Xcode/iOS Simulator for the SwiftUI target). The `ubuntu-latest` jobs do not need a JDK setup step beyond Gradle's own toolchain resolution, since `shared`/`androidApp` only need the JDK to compile.
