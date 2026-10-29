# Northstar — native KMP paper trading

A portfolio and interview project with native Jetpack Compose and SwiftUI screens and shared Kotlin business rules. Practice discovery, search, watchlists, portfolio P/L and reviewed market orders with synthetic Indian stock prices and virtual rupees. **No live prices, brokerage, deposits or real-money orders.**

## Two working modes

- **Offline demo:** no signup or password. Holdings, orders and watchlist stay on the device. Start with ₹10 lakh and illustrative holdings.
- **API paper account:** register with a username and 12+ character password. Each account starts empty with ₹10 lakh. The server owns balances, checks holdings, persists transactions and prevents duplicate orders. Tokens stay in memory; restarting asks you to sign in again. Unconfirmed orders are explicitly retried with their original UUID, including after a restart.

Both modes use INR fixtures. The original backend lab has USD fixtures and a shared account; do not connect its unsecured `/api/v1` ledger to a public release. The backend now lives in [northstar-backend-lab/paper-trading-api](https://github.com/pk-debug/northstar-backend-lab/tree/7bac27ca60fe799a40ec9a11bec7b04d8ff78317/paper-trading-api), preserving the asset/order shapes while introducing authenticated `/api/v2` account isolation. See [API differences](docs/API.md).

## Run

Install **JDK 21 (including javac)**, Android SDK platform `37.0`, build tools `36.0.0`, and use the checked-in Gradle wrapper. Set `ANDROID_HOME` for your SDK. Any trusted JDK vendor works; the previous auto-download-only vendor pin was removed.

```bash
# Offline Android
./gradlew :androidApp:assembleDebug :sharedLogic:testAndroidHostTest

# Authenticated API in the backend repository (Python 3.12)
# Clone beside this app if it is not already checked out:
git clone https://github.com/pk-debug/northstar-backend-lab.git ../northstar-backend-lab
cd ../northstar-backend-lab
# Until the backend change is merged, select its integration branch:
git switch codex/authenticated-paper-api
python -m venv .venv
. .venv/bin/activate
pip install -r paper-trading-api/requirements.lock
NORTHSTAR_DB=/tmp/northstar.sqlite3 uvicorn app.main:app --app-dir paper-trading-api --host 127.0.0.1 --port 8000 --no-access-log

# In another terminal, from the mobile app checkout:
# Android emulator -> host machine, DEBUG ONLY
./gradlew :androidApp:assembleDebug -PapiOrigin=http://10.0.2.2:8000

# From the backend checkout with its virtual environment activated:
PYTHONPATH=paper-trading-api NORTHSTAR_DB=/tmp/northstar-bootstrap.sqlite3 pytest -q paper-trading-api/tests
```

For a physical device or release build use a reachable **HTTPS** API origin, e.g. `-PapiOrigin=https://your-api.example`. Never put broker keys in a mobile build. Omitting `apiOrigin` selects offline demo mode.

**iOS:** open `iosApp/iosApp.xcodeproj` in Xcode on macOS, choose your development team and an iOS 18.2+ device/simulator, and run the `iosApp` scheme. Offline mode works without server configuration. For API mode set the `NORTHSTAR_API_ORIGIN` build setting to your HTTPS origin. In an xcconfig write `https:/$()/your-api.example` because `//` begins a comment. Run shared tests on an Apple Silicon Mac using `./gradlew :sharedLogic:iosSimulatorArm64Test`. iOS cannot be built or simulator-tested on Linux.

## Project map

| Location | Responsibility |
|---|---|
| `androidApp/` | Native Compose UI, Android lifecycle and release configuration |
| `iosApp/` | Native SwiftUI UI, app icon and privacy manifest |
| `sharedLogic/…/features/{auth,trading}/domain/` | Models, search, P/L and local order rules |
| `sharedLogic/…/features/trading/data/remote/` | HTTPS client, account DTOs, timeout and durable retry protocol |
| `sharedLogic/…/features/{auth,trading}/presentation/` | Shared screen state and session composition root |
| `sharedLogic/…/features/trading/broker/` | Future adapter contract and explicit live-execution rejection |
| [northstar-backend-lab/paper-trading-api](https://github.com/pk-debug/northstar-backend-lab/tree/7bac27ca60fe799a40ec9a11bec7b04d8ff78317/paper-trading-api) | Separate repository: isolated accounts, ledger, sessions and API tests |
| `.github/workflows/quality.yml` | Android artifacts, pinned external-backend contract test and macOS build gates |

## Engineering and release guides

- [Feature scope and future work](docs/FEATURES.md)
- [Design principles and contract boundaries](docs/DESIGN-PRINCIPLES.md)
- [Architecture and beginner walkthrough](docs/ARCHITECTURE.md)
- [API contract and backend deployment](docs/API.md)
- [Security decisions and limits](docs/SECURITY.md)
- [Play Store / App Store release checklist](docs/RELEASE.md)
- [Interview guide and resume skill mapping](docs/INTERVIEW.md)
- [Future broker integration](docs/BROKER-INTEGRATION.md)
- [Validation evidence](docs/VALIDATION.md)

This is an interview showcase, not a clone or an endorsement of Zerodha, Groww, Upstox or Angel One. No competitor binary or private API was inspected. The REA repository was reviewed as a possible analysis tool; it supplied no evidence of those companies' internal architectures. Original Northstar branding is used throughout.

Performance/capacity limits and the validation plan are in [PERFORMANCE.md](docs/PERFORMANCE.md). Run `python tools/check_architecture.py` to verify source documentation and domain dependency boundaries.
