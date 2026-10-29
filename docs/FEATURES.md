# Product scope

Northstar uses familiar retail-trading patterns with original branding. It does not promise feature parity with an established broker. The release candidate is a paper-trading interview showcase.

| Capability | Current behavior |
|---|---|
| Discover and search | Seven synthetic INR stocks and three sample indexes; ticker/company search, sector filtering and deterministic A–Z/gainer/loser ranking of synthetic changes |
| Asset detail | Price/change, sector, illustrative sparkline; no historical-data claim |
| Watchlist | Add/remove; local persistence in offline mode or isolated server persistence in API mode |
| Orders | Whole-share synthetic market buys/sells, whole-paise value/cash/affordability preview, review before submission, server rejection for insufficient funds/holdings |
| Reliability | Explicit same-UUID retry after an ambiguous API response, including after restart; no silent queue |
| Portfolio | Cash, holdings, average cost, market value and unrealized P/L; excludes fees/taxes |
| Activity | Completed paper orders; API snapshot includes the latest 500 orders |
| Learning | Shared beginner lessons on market orders, diversification and fees, with native expandable controls |
| Account | Offline demo entry, API signup/login, expiring sessions, sign out, permanent API account deletion |
| Release engineering | Debug APK, unsigned/signed-when-configured AAB, R8, CI definitions, iOS icon/privacy/build configuration |
| Future real execution | Adapter contract and a tested policy that rejects LIVE; no broker implementation |

## Provider-dependent future work

Live market data, real chart intervals, streaming quotes, derivatives/options chains, leverage/margin, IPOs, mutual-fund SIPs, payments, KYC, contract notes, tax reporting and broker order routing are not implemented. Limit/stop orders, open-order cancellation, price alerts, notifications, biometric quick-unlock and password recovery also require additional product work and tests. UI controls are not added for unsupported capabilities.

For a next milestone, choose a specific feature and provider contract rather than adding a fake control. For example, a limit-order simulator needs pending/filled/cancelled states, an event-driven quote source, deterministic matching rules, cash reservation semantics and tests for gaps/partial fills. That is a separate feature from posting an immediate paper market order.

Cloud validation covers compilation, host tests and API behavior. Android emulator/device UX and iOS Xcode/device validation remain release gates; see the release guide.
