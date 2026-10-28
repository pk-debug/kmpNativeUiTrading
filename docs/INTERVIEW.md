# Interview walkthrough

## A defensible introduction

“I built a Kotlin Multiplatform paper-trading app with native Compose and SwiftUI screens. Shared code owns search, portfolio rules and API state. I extended a learning backend contract into authenticated, isolated paper accounts. The most important problem was an order timeout: the server may have committed even when the phone did not get the response. I persist a client UUID before sending, and enforce uniqueness with an atomic server transaction. I deliberately keep real trading disabled.”

Show the code and tests. Do not claim production download numbers, crash-free rates or benchmark improvements for this project without measuring them.

## A five-minute demo

1. Enter the offline demo without giving a real phone number or password. Explain local practice versus server authority.
2. Search TCS, add it to the watchlist, review a buy and cancel the review. Then confirm a buy and show holdings/P/L and activity.
3. In an API-configured build, register two users. Buy in one account and show that the other account remains unchanged.
4. Show `TradingApiTest.aLostResponseSurvivesRestartAndRetriesTheSameOrderKey` and the backend parallel retry test. Explain why automatic re-buy after a timeout would be wrong.
5. Show sign out/session expiry, account deletion, release signing configuration and the live-execution rejection test.

## Resume skill mapping: evidence, not decoration

| Resume area | Evidence here | Honest limit |
|---|---|---|
| Kotlin / Compose / native UI | Android screens, shared models/use cases and SwiftUI bridge | Device accessibility/performance QA remains required |
| MVVM / clean boundaries | Domain models, injected repository/client, AppSession composition root | Constructor DI; no Hilt/Dagger claim |
| Coroutines | Supervised async network scope, cancellation propagation, busy guards | State observers bridge to Swift; no claim of Flow everywhere |
| Offline reliability | Persisted local demo + account-scoped durable retry UUID | No Room cache, WorkManager sync or automatic trade queue |
| REST/API contracts | Versioned DTOs, isolated snapshots, safe errors, Ktor platform engines | Synthetic fixtures; no real quote stream/WebSocket |
| Financial correctness | Integer-paise calculations, transaction lock, scoped idempotency | No fees, taxes, settlement, double-entry ledger or corporate actions |
| Security | Password hashing, session digests/expiry/revocation, HTTPS and server ownership | No MFA, KYC, Play Integrity, pinning or persisted-token Keystore implementation |
| Testing | Shared rules, mocked lost response, parallel backend retries/isolation | Espresso/XCTest device suites and load testing are future work |
| Release / CI | R8, env-driven upload signing, AAB build, macOS workflow and release checklist | Store submission, signing identities and iOS validation need owner resources |
| Production observability | Request IDs, redacted safe errors and mapping retention guidance | No Firebase credentials/Crashlytics/FCM project configured |

Your resume is context for showcasing skills, not proof that an SDK has been integrated into this project. Firebase, payments, video, chat, Aadhaar and push should only be added when there is a real feature, account configuration and testable acceptance criteria. A practice trading app should not collect identity documents merely to demonstrate an SDK.

## Questions to prepare

- **Why not use Double for money?** Binary fractions can introduce tiny errors. The ledger counts whole paise; display values are converted at the boundary.
- **What does idempotency guarantee?** One committed result per account/key/payload while the key is retained. It does not make the whole internet “exactly once.”
- **Why a consistent snapshot?** Separate cash and orders requests can represent different moments. One transaction makes the displayed account coherent.
- **Why no background order replay?** A user must resolve an uncertain result explicitly. Replaying a stale trade later changes their intent.
- **Why SQLite?** It is durable and straightforward for a small single-instance paper service. PostgreSQL and an append-only ledger are the next steps for scale/auditability.
- **How would you measure performance?** Add Macrobenchmark startup/frame timing, representative low-end devices, StrictMode in debug, and trace measurable bottlenecks before claiming gains.
- **How would you add a real broker?** See the adapter checklist. Authentication, partial fills, risk checks and reconciliation matter more than adding an API key.
- **Did you reverse engineer competitors?** No. The REA tool README was inspected, but no competitor binaries/private APIs were analyzed. The UX uses familiar public trading patterns and original branding.
