# Competitor-oriented product review — 29 October 2026

## Evidence boundary

The comparison below uses the product areas supplied by the owner, plus the current Northstar source. Direct requests to Groww, Zerodha, Upstox and Angel One's public sites were denied by the cloud network proxy. Their current prices, chart licenses, onboarding steps, execution speed and reliability were **not independently verified**. No competitor app binaries or private APIs were inspected. Do not copy “fastest”, “best”, ₹0 or a brokerage percentage into Northstar's listing as a verified fact.

## Product gaps and concrete changes

| Comparison lens from the supplied table | Northstar gap | Implemented now | External/future work |
|---|---|---|---|
| Groww: beginners, SIP and clear onboarding | Little help explaining order/risk/cost concepts; no mutual funds | Shared beginner lessons shown with native expandable controls; affordability and holding feedback before order review | Mutual-fund/SIP provider, mandate lifecycle, suitability/legal review and recurring-payment failure handling |
| Kite: active trading and reliability | No discovery rankings; uncertain-result handling must remain explicit | Sector filtering, stable A–Z/gainer/loser ranking of synthetic quotes; existing same-UUID retry and server-authoritative execution preserved | Broker integration, streaming quotes, reconciliation, order-state machine, operational capacity and measured reliability |
| Upstox Pro: charts, day trading and IPO | Illustrative chart only; no live intervals or IPO workflow | Clearly labelled sample rankings and order estimates; current chart remains explicitly illustrative | Licensed live/historical data, chart SDK/license, timeframes, derivatives/advanced orders, IPO eligibility and UPI mandate/status integrations |
| Angel One: research-oriented experience | No learning/research context | Educational lessons on order behavior, diversification and fees; no stock recommendations | Licensed research, source timestamps/disclosures, suitability and regulatory requirements; no invented AI tips |

## New behavior

Discovery combines query and sector filters. A–Z sorts by symbol; gainers sort descending by percentage change and losers ascending, both with deterministic symbol tie-breaks. These are rankings of the bundled/server **synthetic** changes, not live leaderboards. Filters are session-local and do not require a backend migration.

The shared order preview computes whole-paise estimated value, post-order cash, maximum affordable buy quantity and owned shares. Both native detail screens show estimates; invalid buys/sells receive immediate feedback before confirmation. The ViewModel repeats preflight checks so presentation cannot bypass them. The API still reads its own price and authorizes every order; client preview never supplies an execution price. Fees/taxes are excluded and explicitly disclosed. Remote balances can become stale, so preview acceptance does not guarantee server acceptance.

Learning content is identical across native platforms and deliberately educational. It does not provide personalized recommendations, claim diversification eliminates risk, or fabricate current broker fees. Account/order dialogs on Android are scrollable for smaller displays.

## Next priorities before becoming a real competitor

1. Validate the real user journeys on Android/iPhone with accessibility, low-end devices and measured startup/frame traces. The new SwiftUI source still requires Xcode/device testing.
2. Choose a narrow initial segment and a licensed broker/provider. An educational paper app and a regulated brokerage have different obligations.
3. Add trustworthy quotes, timestamps, disconnect/staleness handling, instrument identifiers and an audited order lifecycle before advanced execution.
4. Implement limit/stop/open-order cancellation with matching/reservation rules and tests; do not label a market-order form as a complete trading terminal.
5. Establish identity recovery/MFA, operator controls, PostgreSQL/ledger migrations, observability, staged load/soak tests and disaster recovery.
6. Obtain chart/data licenses and complete broker, privacy, store and regulatory review. No claims of 50K DAU, 100M-user capacity, zero jank or superior execution are established by this UX work.
