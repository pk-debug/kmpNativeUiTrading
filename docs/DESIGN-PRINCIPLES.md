# Design principles and their concrete boundaries

These are design choices with regression checks, not a blanket certification that every future change satisfies SOLID.

| Principle | Current implementation | Evidence / boundary |
|---|---|---|
| Single responsibility | ViewModels publish screen state; local/server session adapters own execution mode. The HTTP facade owns wire requests; PendingOrderJournal owns retry persistence. | Session adapters and the journal can be exercised without native UI. HTTP DTOs remain together because they describe one API contract. |
| Open/closed | AppSession selects injected AuthSession and TradingSession implementations once. ViewModels do not test a nullable remote client to choose behavior. | A new implementation can implement the contracts; composition must explicitly opt into it. This does not automatically enable a broker. |
| Liskov substitution | Local/server sessions share refresh/command/state semantics and domain failures. | SessionContractTest runs common account journeys against both trading adapters and auth adapters. This proves the tested behaviors, not every possible implementation. |
| Interface segregation | AuthGateway and AccountGateway separate network identity from trading. Screen-facing session contracts separate auth and trading lifetimes. | Further splits should follow an actual consumer need, rather than adding one-method interfaces everywhere. |
| Dependency inversion | Presentation uses domain contracts and AccountFailure/FailureKind. The composition root knows concrete data adapters. | CI rejects data, HTTP and platform-storage references in presentation, including qualified references. |
| DRY | One CalculatePortfolioUseCase owns valuation and missing-price behavior. Android palette tokens live in core/design; SwiftUI components/formatting share core files. | Missing quotes use cost consistently and are tested. Android and iOS rendering remain native; similar UI code across languages is not forced into an unsuitable abstraction. |
| Composition over inheritance | AppSession assembles strategies and gateways. ViewModels do not inherit a generic base ViewModel for shared lifecycle code. | Framework-required Activity/ViewModel/NSObject inheritance and typed exception inheritance remain intentional. |
| Separation of concerns | SwiftUI screens, observable bridges, navigation and components have separate files. Auth/trading presentation is separate from persistence, HTTP and domain rules. | File organization alone is insufficient; the dependency guard enforces the important direction. |
| Encapsulate what varies | Local validation/persistence and server synchronization live in separate session adapters. Error transport details stay in the HTTP layer. | Local/server capability differences are explicit through mode metadata and safe domain errors, rather than guessing from API keys. |
| Cohesion / coupling | Related auth behavior lives under auth; trading under trading; shared failure vocabulary, composition and design tokens live in core. | Core remains small. No generic utility module or deep inheritance tree is introduced. |

## Contract details

Trading clients refresh before account-changing commands. Commands either return the updated ledger, throw a safe domain failure, or propagate cancellation. Rejected commands do not mutate account state. Watchlist addition is idempotent. Pending-order retry semantics are a server capability; the local adapter reports a safe error when no pending order exists. Account reset clears private remote view state while offline practice data remains on device.

Auth adapters accept their mode's valid identities and return the normalized identity. Sign-out removes local access; a failed server revocation yields an explicit warning rather than leaving the phone signed in. Offline mode has no server registration/deletion capability and reports this safely if called; UI mode metadata controls the visibility of those controls. These differences are part of the contract, not promises that all backends support every operation.

## Validation and remaining work

Run `python tools/check_architecture.py` and `python -m unittest discover -s tools/tests`. Negative guard tests cover aliased imports, qualified-constructor bypasses and missing documentation. Shared Kotlin tests cover adapter contracts, missing quotes, storage-free retry reads, cache reuse, bounded local history, ambiguous-response retries and sign-out failures. The live HTTP contract test verifies the real wire integration separately from the adapter fakes.

SwiftUI source is split without changing its public shared-model bridge names. Xcode/device validation still requires a Mac. Offline native persistence still has synchronous operations; this design cleanup does not certify zero jank, backend capacity or store readiness.
