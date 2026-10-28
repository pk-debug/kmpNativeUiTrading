# A future broker integration

The working product is paper trading. `ExecutionPolicy` rejects LIVE unconditionally, and no UI switch can bypass it. `BrokerAdapter` defines submit, cancel and reconcile, but no broker adapter is implemented or certified. It is a design seam, not an API-key-only integration.

A real integration requires a backend implementation for the chosen broker's official documented API. Keep its client secret in a server secret store. The phone opens the broker's supported OAuth/login flow and receives only your application's scoped session. Do not put a broker API key in BuildConfig, Info.plist, source control, a deep link or a downloadable mobile artifact.

## Adapter acceptance checklist

1. Confirm supported instruments, exchanges, order types, product types, market hours, lot sizes, tick sizes and broker/platform eligibility.
2. Implement broker authentication, redirect validation, token expiry/rotation and logout/revocation. Confirm broker developer-account permissions and fees.
3. Map canonical instrument IDs rather than guessing from ticker strings. Add authoritative quotes, timestamps and feed-disconnection handling.
4. Model order states separately: accepted, open, partially filled, filled, rejected, cancel-requested and cancelled. A successful POST is not a fill.
5. Store your client order ID alongside broker order IDs. Handle a timeout without resubmitting an unknown live order; use broker-supported lookup/idempotency and reconciliation.
6. Add funds/margin checks, price collars, quantity/notional limits, per-account kill switches, audit events and operator alerts. Never replay old paper orders to a broker.
7. Test duplicate callbacks, reordered fills, partial fills, expiry, reconnects, rejected cancels and broker outages in the broker's sandbox or approved testing process.
8. Obtain relevant broker/exchange/regulatory approval and update store disclosures before enabling LIVE in a separately reviewed release.

Once an approved adapter exists, selecting a configured provider can be a small operational change. Building and validating the adapter is not a five-minute task. Zerodha Kite Connect, Upstox and Angel One have different contracts and policies; their current official docs must be checked when a provider is chosen. This project makes no claim to have tested them.
