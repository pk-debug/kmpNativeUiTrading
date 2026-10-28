# Connect the native app to its backend

The API implementation, database, tests, Dockerfile and Compose configuration live in **[northstar-backend-lab/paper-trading-api](https://github.com/pk-debug/northstar-backend-lab/tree/7bac27ca60fe799a40ec9a11bec7b04d8ff78317/paper-trading-api)**. The mobile repository contains the shared Kotlin client and native screens. There is no bundled server in the APK or iOS app.

See the [backend API/deployment guide](https://github.com/pk-debug/northstar-backend-lab/tree/7bac27ca60fe799a40ec9a11bec7b04d8ff78317/paper-trading-api/README.md) for endpoints, account isolation, sessions, idempotency, SQLite operation and hosting requirements. The original five lab services remain separate, unauthenticated v1 USD demos. This native app uses the authenticated v2 INR paper service.

## Run side by side

Check out `northstar-backend-lab` beside this mobile repository and select its `codex/authenticated-paper-api` branch until merged. From the backend repository:

```bash
python -m venv .venv
. .venv/bin/activate
pip install -r paper-trading-api/requirements.lock
NORTHSTAR_DB=/tmp/northstar.sqlite3 uvicorn app.main:app --app-dir paper-trading-api --host 127.0.0.1 --port 8000 --no-access-log
```

From the mobile repository, build the emulator client:

```bash
./gradlew :androidApp:assembleDebug -PapiOrigin=http://10.0.2.2:8000
# Run the real HTTP client contract test against that separate server:
NORTHSTAR_TEST_API=http://127.0.0.1:8000 ./gradlew :sharedLogic:testAndroidHostTest
```

For physical devices and release builds, use a reachable HTTPS origin. iOS uses `NORTHSTAR_API_ORIGIN`. Offline demo mode requires no backend.

## CI ownership

The backend repository owns Python tests and dependency auditing. Mobile CI checks out backend commit `7bac27ca60fe799a40ec9a11bec7b04d8ff78317` into `.backend/` and runs the native client's real HTTP account journey against it. Update the pinned SHA after validating a future API change. This cross-repository checkout works before either branch is merged and avoids accidentally testing unrelated future backend changes.
