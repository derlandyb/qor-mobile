<a id="readme-top"></a>

[![Issues][issues-shield]][issues-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![License: TBD][license-shield]][license-url]

<br />
<div align="center">
  <h3 align="center">qor-mobile</h3>

  <p align="center">
    QOR's native mobile app — Kotlin Multiplatform shared domain, Jetpack Compose on Android, SwiftUI on iOS.
    <br />
    <a href="https://github.com/derlandyb/QOR/tree/main/.specs"><strong>Explore the specs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/derlandyb/QOR">Root QOR repo</a>
    &middot;
    <a href="https://github.com/derlandyb/qor-mobile/issues/new?labels=bug">Report Bug</a>
    &middot;
    <a href="https://github.com/derlandyb/qor-mobile/issues/new?labels=enhancement">Request Feature</a>
  </p>
</div>

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a>
      <ul><li><a href="#built-with">Built With</a></li></ul>
    </li>
    <li><a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project

<!-- [product-screenshot]: no screenshot yet — no application code exists in this repo yet, this is scaffolding only -->

`qor-mobile` is **QOR**'s fan-facing native app for Android and iOS — no-login event browsing across Greater Vitória (Vitória, Vila Velha, Serra, Cariacica), account/auth, favorites, friends, and notifications.

The repo is one Kotlin Multiplatform project with three modules:

* **`shared`** — Clean Architecture domain layer (entities, use cases, repository interfaces), zero framework dependency, consumed by both platform UIs
* **`androidApp`** — Jetpack Compose UI implementing QOR's NIGHTLIFE-GV design system
* **`iosApp`** — SwiftUI UI, same design system re-derived per platform

Per the root repo's git convention, shared/Android/iOS changes are always committed separately — never mixed in one commit.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

* [![Kotlin][Kotlin.com]][Kotlin-url]
* [![Jetpack Compose][Compose.com]][Compose-url]
* [![Swift][Swift.com]][Swift-url]
* [![SwiftUI][SwiftUI.com]][SwiftUI-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Getting Started

The Shared module foundation (S1–S11: KMP scaffold, design tokens, domain layer, event/user models, auth, session store, polling) is implemented. Android/iOS product UI (A1–A14, I1–I14) is not yet built — `androidApp`/`iosApp` are bare shells just large enough for the project graph to compile and link.

### Prerequisites

* JDK 17 (`java -version`)
* Gradle — this repo uses the Gradle **wrapper**, so a system install isn't required, but one via Homebrew (`brew install gradle`) works too for one-off commands
* Android SDK with platform 36 + build-tools installed (`sdkmanager`, or install via Android Studio) — `ANDROID_HOME`/`local.properties` must point at it
* Xcode 16+ (macOS only, for `iosApp`) with command-line tools selected (`xcode-select -p`)
* [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`) — the iOS project (`iosApp/iosApp.xcodeproj`) is generated from `iosApp/project.yml`, not hand-maintained/committed as raw pbxproj

This is the **one** QOR repo that does *not* run through Docker Compose — Android/iOS toolchains aren't containerized the way the four web/API services are.

**Toolchain note**: AGP 9.x removes the classic `androidTarget()` KMP integration in favor of a new `com.android.kotlin.multiplatform.library` plugin, and is also incompatible with Gradle ≥9.6 for the classic `com.android.library`/`com.android.application` plugins (a Gradle-internal API AGP 8.x depends on was removed in 9.6). Until the KMP/AGP 9.x migration path is adopted, this repo pins **Gradle 9.5.0** (via the wrapper) with **AGP 8.13.2** — the last combination where `androidTarget()` KMP + `com.android.library`/`com.android.application` work together without extra flags.

### Installation

```sh
git clone https://github.com/derlandyb/qor-mobile.git
cd qor-mobile

# Android + shared (uses the Gradle wrapper — no local Gradle install needed)
./gradlew build            # shared + androidApp
./gradlew test              # commonTest, run on the jvm() target — fast, no emulator
./gradlew detekt

# iOS — regenerate the Xcode project from project.yml, then build
cd iosApp && xcodegen generate && cd ..
xcodebuild -scheme iosApp -project iosApp/iosApp.xcodeproj build
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Usage

This repo is implemented task-by-task from the root `QOR` repo's spec-driven plan. See [`.specs/tasks/mobile.md`](https://github.com/derlandyb/QOR/blob/main/.specs/tasks/mobile.md) for the full breakdown split by Shared / Android / iOS, and [`.specs/features/`](https://github.com/derlandyb/QOR/tree/main/.specs/features) for the requirements each task traces back to.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ROADMAP -->
## Roadmap

- [ ] **MVP Core** — repo scaffolding, NIGHTLIFE-GV design-system component library (Compose + SwiftUI), auth screens, event discovery/detail
- [ ] **Social & Notifications** — favorites, friends list, in-app share, notification preferences, social feed
- [ ] *(Monetization has no mobile UI — organizer/plan management lives in `qor-admin`, plan pricing lives in `qor-landingpage`)*

See the [open issues](https://github.com/derlandyb/qor-mobile/issues) for a full list of proposed features (and known issues), and the root repo's `.specs/project/ROADMAP.md` for milestone status.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing

Contributions make the open source community an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion, please fork the repo and create a pull request. You can also simply open an issue. Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- LICENSE -->
## License

No license has been chosen yet for this project. All rights reserved until a license is added.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Contact

Derlandy Belchior - derlandy.belchior@gmail.com

Project Link: [https://github.com/derlandyb/qor-mobile](https://github.com/derlandyb/qor-mobile)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

* [Best-README-Template](https://github.com/othneildrew/Best-README-Template)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
[issues-shield]: https://img.shields.io/github/issues/derlandyb/qor-mobile.svg?style=for-the-badge
[issues-url]: https://github.com/derlandyb/qor-mobile/issues
[forks-shield]: https://img.shields.io/github/forks/derlandyb/qor-mobile.svg?style=for-the-badge
[forks-url]: https://github.com/derlandyb/qor-mobile/network/members
[stars-shield]: https://img.shields.io/github/stars/derlandyb/qor-mobile.svg?style=for-the-badge
[stars-url]: https://github.com/derlandyb/qor-mobile/stargazers
[license-shield]: https://img.shields.io/badge/license-TBD-lightgrey.svg?style=for-the-badge
[license-url]: #license
[Kotlin.com]: https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white
[Kotlin-url]: https://kotlinlang.org
[Compose.com]: https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white
[Compose-url]: https://developer.android.com/jetpack/compose
[Swift.com]: https://img.shields.io/badge/Swift-F05138?style=for-the-badge&logo=swift&logoColor=white
[Swift-url]: https://www.swift.org
[SwiftUI.com]: https://img.shields.io/badge/SwiftUI-0066CC?style=for-the-badge&logo=swift&logoColor=white
[SwiftUI-url]: https://developer.apple.com/xcode/swiftui/
