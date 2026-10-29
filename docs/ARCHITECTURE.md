# Architecture: explain it to a 15-year-old

Think of the UI as a restaurant menu, the shared Kotlin code as the waiter, and the API as the cashier. The menu never decides how much money an account has. The cashier checks the balance and records a trade before saying it succeeded.

## An order's journey

1. A user selects a stock and enters a whole-share quantity.
2. A review screen shows BUY/SELL, shares, estimated value and the paper-trading label. The user confirms.
3. In offline mode, `PlaceOrderUseCase` checks sample cash/holdings, computes amounts in whole paise and saves the new ledger. This is practice data under the device owner's control.
4. In API mode, `RemoteAccount` saves a UUID and order payload before sending anything. The retry record is scoped to API origin and username. Passwords and tokens are excluded.
5. The API derives the user from a bearer session. Clients cannot choose someone else's account ID or price. SQLite takes a write lock before it reads cash or holdings.
6. The same database transaction checks the UUID, changes cash/positions and adds the order. It commits all three or none.
7. A single snapshot reads cash, holdings, orders and watchlist consistently. Kotlin maps that snapshot into the state both native UIs consume.
8. If a response is lost, the app preserves the UUID. The user explicitly chooses retry. The backend returns the original result instead of spending twice.

A timeout means **“we do not know yet,”** not “the trade failed.” Orders are never silently replayed in the background. There is no WorkManager order queue because executing a trade after the user leaves would be surprising.

## State, lifetimes and dependencies

`AppSession` is the composition root. Constructor arguments make dependencies visible without hiding platform dependencies inside domain code. Android retains it in a lifecycle ViewModel through rotation. SwiftUI owns observable bridges through StateObject. Callbacks adapt shared Kotlin state to native UI bindings; they are not a claim that this project uses Flow everywhere. Remote operations run through a supervised main coroutine scope with a busy guard; HTTP engines do network work off the UI thread. Cancellation is rethrown rather than shown as a network error.

Local ledger serialization accepts an absent watchlist, so existing demo data can still load. API snapshots remain in memory for read-only viewing during a temporary outage. The app does not claim to have a persistent Room offline cache or a distributed synchronization engine.

No fake “live” badges or invented historical chart data: the trend line is explicitly illustrative. Search is local over the received catalog. Watchlists persist locally or per API account. P/L is unrealized and excludes fees, taxes and corporate actions.

## Deliberate limits

The server is a single-instance SQLite paper exchange. BEGIN IMMEDIATE serializes writers; that is simple and testable at this scale. It is not a low-latency exchange or horizontally scalable trading system. A real service would use PostgreSQL, versioned migrations, append-only cash/position ledger entries, pagination, reconciliation workers, measured SLOs and operational monitoring. Snapshot history currently returns the latest 500 orders; a full statement/export endpoint is future work.

The native UI layers remain separate because platform accessibility, navigation and release lifecycles differ. Shared domain/network behavior prevents two versions of order rules. We did not add Hilt, Room, Firebase, WorkManager or a broker SDK solely to inflate a skills list.


## Feature boundaries

Shared source is organized into `features/auth` and `features/trading`, each with domain and presentation packages. Trading's HTTP implementation lives in `features/trading/data/remote`, and local ledger serialization in `features/trading/data/local`. `core/session` is the composition root and `core/platform` supplies native persistence and engines. View models depend on domain AuthSession and TradingSession contracts; the root composes local/server strategies. Server strategies adapt AuthGateway and AccountGateway interfaces. Domain AccountFailure/FailureKind describes safe failures without HTTP statuses. Domain interfaces expose domain values, without importing Ktor or persistence.

Android auth and trading UI now live in separate feature packages. SwiftUI screens now live under Features/Auth and Features/Trading; observable bridges and reusable components live under Core. ContentView composes navigation. Source headers document file responsibilities, and a CI architecture check prevents domain dependencies on data/presentation/platform implementations. JSON/assets/lockfiles are schema/data artifacts and do not receive invalid comment syntax.
