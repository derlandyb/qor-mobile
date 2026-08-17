.DEFAULT_GOAL := help

.PHONY: help format lint static-analysis unit-tests test ui-e2e-tests build coverage ios-generate

help:
	@printf '%s\n' 'qor-mobile commands:' \
		'  make format            apply ktlint formatting (Kotlin)' \
		'  make lint               check ktlint + Android lint' \
		'  make static-analysis    run detekt (Kotlin) + SwiftLint (iOS, if installed)' \
		'  make unit-tests         run the shared KMP + androidApp unit test suites' \
		'  make test               alias for unit-tests' \
		'  make ui-e2e-tests       run Compose instrumented tests (Android) + XCTest (iOS)' \
		'  make build              assemble the Android debug APK and build the iOS app' \
		'  make coverage           run shared-module coverage (Kover) with the 80% gate' \
		'  make ios-generate       regenerate iosApp.xcodeproj from iosApp/project.yml via xcodegen'

format:
	./gradlew ktlintFormat

lint:
	./gradlew ktlintCheck :androidApp:lintDebug

static-analysis:
	./gradlew detekt
	@if command -v swiftlint >/dev/null 2>&1; then swiftlint lint iosApp; else echo 'swiftlint not installed, skipping iOS static analysis'; fi

unit-tests:
	./gradlew :shared:testDebugUnitTest :androidApp:testDebugUnitTest

test: unit-tests

ios-generate:
	cd iosApp && xcodegen generate

ui-e2e-tests:
	./gradlew :androidApp:connectedDebugAndroidTest
	$(MAKE) ios-generate
	cd iosApp && xcodebuild test -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16'

build:
	./gradlew :androidApp:assembleDebug
	$(MAKE) ios-generate
	cd iosApp && xcodebuild build -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16'

coverage:
	./gradlew :shared:koverVerifyDebug :shared:koverXmlReportDebug
