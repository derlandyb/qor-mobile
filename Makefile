.DEFAULT_GOAL := help

.PHONY: help format lint static-analysis static-analysis-jvm static-analysis-ios unit-tests test \
	ui-e2e-tests android-e2e-tests ios-e2e-tests build android-build ios-build coverage ios-generate

help:
	@printf '%s\n' 'qor-mobile commands:' \
		'  make format               apply ktlint formatting (Kotlin)' \
		'  make lint                 check ktlint + Android lint' \
		'  make static-analysis      run detekt (Kotlin) + SwiftLint (iOS, if installed)' \
		'  make static-analysis-jvm  run detekt (Kotlin) only' \
		'  make static-analysis-ios  run SwiftLint (iOS) only' \
		'  make unit-tests           run the shared KMP + androidApp unit test suites' \
		'  make test                 alias for unit-tests' \
		'  make ui-e2e-tests         run Compose instrumented tests (Android) + XCTest (iOS)' \
		'  make android-e2e-tests    run Compose instrumented tests (Android) only' \
		'  make ios-e2e-tests        run XCTest (iOS) only' \
		'  make build                assemble the Android debug APK and build the iOS app' \
		'  make android-build        assemble the Android debug APK only' \
		'  make ios-build            build the iOS app only' \
		'  make coverage             run shared-module coverage (Kover) with the 80% gate' \
		'  make ios-generate         regenerate iosApp.xcodeproj from iosApp/project.yml via xcodegen'

format:
	./gradlew ktlintFormat

lint:
	./gradlew ktlintCheck :androidApp:lintDebug

static-analysis: static-analysis-jvm static-analysis-ios

static-analysis-jvm:
	./gradlew detekt

static-analysis-ios:
	@if command -v swiftlint >/dev/null 2>&1; then swiftlint lint iosApp; else echo 'swiftlint not installed, skipping iOS static analysis'; fi

unit-tests:
	./gradlew :shared:testDebugUnitTest :androidApp:testDebugUnitTest

test: unit-tests

ios-generate:
	cd iosApp && xcodegen generate

android-e2e-tests:
	./gradlew :androidApp:connectedDebugAndroidTest

ios-e2e-tests: ios-generate
	cd iosApp && xcodebuild test -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17'

ui-e2e-tests: android-e2e-tests ios-e2e-tests

android-build:
	./gradlew :androidApp:assembleDebug

ios-build: ios-generate
	cd iosApp && xcodebuild build -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17'

build: android-build ios-build

coverage:
	./gradlew :shared:koverVerifyDebug :shared:koverXmlReportDebug
