# Release checklist

The repository produces Android debug and release artifacts and contains iOS source/configuration. “Buildable” and “approved by an app store” are different milestones. Signing, a public HTTPS service, store accounts, policy forms and device QA are owner inputs. No store submission, account purchase or public deployment is automated here.

## Android / Play Console

1. Choose the permanent application ID, brand, support contact, content rating, privacy policy URL and account-deletion website URL. Verify target SDK requirements in current Play policy at submission time. The project targets API 37, min API 24.
2. Choose offline demo or API mode. An API release uses a stable reachable HTTPS origin. An empty `apiOrigin` produces offline practice. Never ship an emulator origin or a broker credential.
3. Enable Play App Signing. Keep the upload keystore and passwords outside the repo, supplied securely as `NORTHSTAR_KEYSTORE_PATH`, `NORTHSTAR_KEYSTORE_PASSWORD`, `NORTHSTAR_KEY_ALIAS`, `NORTHSTAR_KEY_PASSWORD`.
4. Run gates, then build a monotonically increasing version:

```bash
./gradlew :sharedLogic:testAndroidHostTest :androidApp:lintDebug :androidApp:bundleRelease \
  -PapiOrigin=https://your-api.example \
  -PreleaseVersionCode=2 -PreleaseVersionName=1.0.1
```

5. The AAB is `androidApp/build/outputs/bundle/release/androidApp-release.aab`. Without signing variables it is **unsigned**, suitable for CI inspection but not Play upload. Verify signatures with `jarsigner -verify -verbose -certs <aab>` after supplying your keystore. Do not create a disposable production key inside this chat.
6. Archive the matching `androidApp/build/outputs/mapping/release/mapping.txt`. R8 and resource shrinking are enabled. Check native-library alignment/16 KiB compatibility with Play's validation tools and real devices; successful bundle packaging alone does not prove compatibility.
7. Install through internal testing. Exercise signup, invalid credentials, watchlist persistence, review/cancel, buy/sell, P/L, sign out, account deletion, process death after submitting, network loss, font scaling, TalkBack, small screens, back navigation and rotation. Test min-supported and current OS versions. Capture original screenshots from the running app.
8. Complete Data Safety and financial-features declarations honestly: synthetic education/paper trading, no real-money brokerage. Declare API username and practice activity as applicable; explain encryption, retention and deletion. Include review instructions and reachable demo credentials if required. Complete any new-account testing requirements shown by Play Console.

## iOS / App Store Connect

1. Use a Mac with supported Xcode, JDK 21 and Android SDK tooling for Gradle configuration. Select your Apple development team in `iosApp/Configuration/Config.xcconfig`. Use a permanent unique bundle ID; the old team-ID-concatenated identifier has been corrected.
2. Build offline or set `NORTHSTAR_API_ORIGIN` to your HTTPS backend. For xcconfig syntax use `https:/$()/your-api.example` to avoid treating `//` as a comment. Do not add broad App Transport Security exceptions.
3. Run shared simulator tests on an Apple Silicon Mac, build/run the SwiftUI app and exercise the same scenarios as Android with VoiceOver and Dynamic Type. Linux validation cannot replace this step.
4. Verify the original 1024 px Northstar icon and `PrivacyInfo.xcprivacy` are present in the archived bundle. The manifest records app-specific UserDefaults access and API-mode user ID/practice content. Recheck required-reason API usage and third-party manifests in the final archive.
5. Increment `CURRENT_PROJECT_VERSION` and `MARKETING_VERSION`. Archive from Xcode for a generic iOS device, validate, sign with your distribution profile and upload to TestFlight. Certificates/profiles must be entered securely outside source control.
6. Complete App Privacy, age rating, financial-app declarations, support/privacy/deletion URLs, screenshots, review notes and export-compliance answers appropriate to your final distribution and use of standard OS TLS. Test account deletion in-app. If recovery/account authentication changes, reassess applicable sign-in rules.
7. Run a TestFlight beta before submitting. App Store approval cannot be guaranteed by source changes.

## Suggested listing copy

**Name:** Northstar Paper Trading

**Short description:** Practice stock orders with virtual rupees and illustrative market data.

Discover sample Indian stocks, build a watchlist and practice buy/sell decisions. Review orders before confirming, track practice holdings and see unrealized gains and losses. Northstar is an educational simulation. Prices and trends are synthetic; it does not place exchange orders, accept deposits, provide live market data or offer investment advice.

Replace this draft with your actual support/privacy URLs. Do not claim competitor affiliation, live data, guaranteed returns, regulated brokerage, or security certifications.
