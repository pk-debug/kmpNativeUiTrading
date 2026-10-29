# Validation evidence — 28 October 2026

Validated in the Linux cloud workspace. No public backend deployment, store submission, signed store release or macOS execution was performed.

| Check | Outcome |
|---|---|
| API tests (`pytest paper-trading-api/tests` in the backend repository) | 11 passed |
| Shared Android host tests (`testAndroidHostTest`) | 16 executed, zero failures/errors/skips, including the real HTTP contract journey |
| Android offline debug APK | Built successfully |
| Android API/emulator debug APK | Built with `apiOrigin=http://10.0.2.2:8000` |
| Android release AAB | Built with R8/resource shrinking; **unsigned**, no upload key supplied |
| Android lint | Passed with zero errors and 16 nonfatal warnings, primarily dependency-version advice and legacy icon/resource conventions |
| Release HTTP guard | `preReleaseBuild` with HTTP API origin rejected as expected |
| Debug APK alignment | `zipalign -c -P 16 4` passed; this is an APK ZIP alignment check, not a complete device/native-library compatibility certification |
| Python dependency audit | `pip-audit 2.10.1`, PyPI advisory service, pinned full graph: no known vulnerabilities reported |
| Actual backend readiness | HTTP health returned 200/ok with a database check |
| XML/plist/CI config | Android XML, iOS Info/Privacy plists and workflow YAML parsed |
| Reusable cloud installer | Executed successfully end to end, including its temporary API and live Kotlin contract test |
| iOS build/simulator/device tests | **Not run**: this environment is Linux |
| Android emulator/device UX tests | **Not run**: no device validation was performed |
| GitHub Actions | Workflow definitions added; no remote workflow run claimed |

The API suite checks user isolation, exact balances, same-key retries, simultaneous retries, concurrent distinct orders that cannot overspend, rejected-order immutability, buy/sell round trips, private watchlists, password/token hashing, expiry/revocation, account deletion cascades, rate limits, and password-safe validation errors.

Shared tests cover existing domain behavior plus watchlist persistence across orders, live-mode rejection, HTTPS origin enforcement, lost-response UUID reuse across a recreated client, per-account retry isolation, safe server-error handling, local sign-out after a revocation outage, and an actual Kotlin/OkHttp -> FastAPI account journey. The opt-in live contract test accepts only a loopback host. Its URL is declared as a Gradle test input so a cached unit-only run cannot hide it.

The initial Python versions had published advisories. They were upgraded, locked, retested and re-audited. The audit scope is the Python graph, not a claim that every mobile/OS dependency has been audited. Backend tests currently emit a test-client deprecation warning recommending a future httpx2 migration; all assertions executed successfully.

The broad `allTests` task attempted native-toolchain preparation on Linux. The validated Linux command uses `testAndroidHostTest`; iOS simulator execution is reserved for an Apple Silicon Mac. A macOS compile/Xcode build job is defined in CI but remains unexecuted here.

## Generated artifacts

Under the checkout's ignored `build/artifacts/` directory:

- `northstar-offline-debug.apk`: installable debug-signed offline demo.
- `northstar-api-emulator-debug.apk`: debug build configured for an Android emulator's host API on port 8000.
- `northstar-offline-release-unsigned.aab`: release bundle for inspection; supply the documented upload signing configuration before Play upload.

See `docs/RELEASE.md` for device QA, hosting, signing, privacy disclosures, store accounts and submission requirements. These artifacts do not establish store readiness or competitor feature parity.


## Backend repository separation — 29 October 2026

The service was moved to `northstar-backend-lab/paper-trading-api` without changing its API behavior. Python tests/auditing belong to that repository; the mobile workflow pins the backend commit for its HTTP contract test. Local cloud installation/start instructions use the sibling backend checkout. The original five v1 lab implementations were preserved.

## Feature architecture and performance hardening — 29 October 2026

The shared code is now organized into auth/trading features with pure domain gateway interfaces, separate data implementations and MVVM presentation. Android auth/trading UI has separate feature packages. A source-documentation and domain-boundary check passes for 41 Kotlin/Swift source files.

The host suite now executes 17 tests, including regression assertions that retry-state reads do not touch storage, search reuses portfolio valuation, and local history retains only the latest 500 entries. The moved backend's 11 tests also pass. A loopback-only read-load probe and debug-only Android frame-duration diagnostics are included; the device collector has not been exercised on a phone.

`docs/PERFORMANCE.md` records the local catalog-only probe and its limitations. No 50K-DAU capacity, 100M-user capacity or zero-jank guarantee was established. There is no deployed environment or mobile-store build available for production/device measurements. iOS feature/package integration still requires a Mac/Xcode build and device QA.


## SOLID/DRY responsibility cleanup — 29 October 2026

Local/server execution moved behind domain AuthSession and TradingSession contracts. ViewModels no longer import transport failures or choose behavior through a nullable HTTP client. Retry persistence moved into PendingOrderJournal; valuation and missing-price behavior have one use case. Android shares design tokens; SwiftUI responsibilities now have separate files. Architecture guard tests reject presentation-to-data imports and qualified-reference bypasses. See DESIGN-PRINCIPLES.md for exact contracts and remaining limitations.
