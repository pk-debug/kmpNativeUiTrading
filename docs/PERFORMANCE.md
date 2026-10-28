# Performance and capacity assessment

## Current conclusion

There is no production deployment URL, device frame trace, hosting specification or DAU workload evidence available in this task. App Store/Play Store distribution does not provide backend capacity. **Neither 50K DAU readiness nor 100M-user scalability is established.** No code change can honestly guarantee zero lag on every device/network.

The current separate backend uses one SQLite database and `BEGIN IMMEDIATE` for transactions, including private reads/session lookup. Writers serialize across all accounts. Auth hashing consumes CPU/memory, snapshots include up to 500 orders, and no production load-balancing, database failover or autoscaling has been demonstrated. This is a paper-service architecture, not a 100M-user platform.

## Mobile improvements implemented

- Retry-state reads now use memory; authentication restores the saved record once. Retry persistence/JSON work runs on a worker dispatcher, with an injectable dispatcher for deterministic tests.
- Portfolio valuation builds a symbol-to-price map and caches by ledger/catalog identity. Search, progress and message updates reuse valuation rather than repeatedly scanning all assets for every holding.
- Repeated search text does not publish a new state; search input is bounded to 100 characters.
- Offline order history retains the latest 500 entries, bounding ledger serialization and activity rendering. Older local history is discarded; this is not a regulatory statement/archive feature.
- SwiftUI uses lazy vertical stacks and reuses its currency formatter instead of allocating a formatter for each displayed amount.
- Auth and trading have feature packages and domain gateway interfaces. Platform construction lives in core; CI checks the dependency boundary and source-file documentation.

**Remaining mobile bottleneck:** local demo initialization and ledger/session persistence still use synchronous native storage APIs in places. No claim is made that all disk access has been removed from the UI thread. A fully asynchronous local repository, startup loading state, and persistent paged database are appropriate next steps before growing the local dataset. These changes need device profiling and platform tests, not an unmeasured assertion of improvement.

## Local read probe (not capacity certification)

Run `python tools/load_read_api.py --origin http://127.0.0.1:8000 --requests 500 --concurrency 20` against a dedicated local backend. The tool accepts loopback only, validates responses and exits nonzero on failures. It can exercise authenticated snapshot reads with `--endpoint /api/v2/snapshot` and a session supplied in `NORTHSTAR_LOAD_TOKEN`; tokens/bodies are never printed.

One local cloud run against **public static catalog reads only** completed 500 requests at concurrency 20 with zero errors: 401.81 requests/sec, p50 42.22 ms, p95 79.83 ms, p99 131.09 ms. This short same-machine experiment does not measure writes, password hashing, multiple accounts, network latency, database growth, peak endurance or phone rendering. It cannot be extrapolated to DAU. Re-run on dedicated staging hardware with a documented workload before making capacity claims.

## Translate user counts into a workload

Assuming 20 API requests per daily active user and a 20x peak-to-average factor:

| Scenario | Average requests/sec | Illustrative peak requests/sec |
|---|---:|---:|
| 50,000 DAU | 11.6 | 232 |
| 100 million DAU | 23,148 | 462,963 |

These are assumptions, not observed traffic. 100 million registered users is different from 100 million DAU. Streaming connections, order rates, retained history, geography and login storms need separate budgets.

## Release measurements required

Use release/profileable builds on a low-end Android phone and supported iPhones. Add Android Macrobenchmark startup/scroll/order-review journeys using FrameTimingMetric and startup metrics, and collect Perfetto/JankStats traces; use Instruments Animation Hitches/Time Profiler on iOS. Test long lists, maximum font sizes, cold/warm startup, rotation, process death and slow/disconnected networks. Record device/OS/build ID, iterations, p50/p95/p99 and missed-frame/hitch percentages. At 60 Hz a frame interval is about 16.7 ms; at 120 Hz it is 8.3 ms. Targets must be agreed and measured. No such device traces were captured in this Linux workspace.

For 50K DAU staging validation: provision distinct accounts safely, seed realistic history, run mixed catalog/snapshot/watchlist/order traffic with unique idempotency keys, verify cash/position invariants, and sustain the projected peak plus headroom. Include duplicate retries, reconnect/login storms, soak tests, disk saturation and dependency failures. Measure error rate, p95/p99 latency, queue depth, CPU/RAM, connection pools and lock wait time. Do not run mutating load against real-money services.

## Architecture needed before large scale

Move authoritative balances to a transactional database with migrations, account-scoped locking and append-only audited ledger entries. Paginate order history, index account queries, and split public catalog/quote delivery from private transactions. Use CDN/cache for public data; never share private account caches across users. Partition by account only after measuring contention. Introduce stateless API replicas, managed session storage/identity, bounded pools/queues, request limits, idempotency retention, read replicas where consistency permits, observability, backups/restoration and regional failover exercises. At very large scale, partition streaming delivery and reconcile order events independently from mobile snapshots.

A PostgreSQL migration, distributed infrastructure and device benchmarks are not implemented by this refactor. They require the intended deployment, traffic model and operational ownership. Keep real trading disabled until its separate broker/risk/reconciliation gates are complete.

Android debug builds also collect aggregate `FrameMetrics` durations on a worker thread and emit a `NorthstarFrames` log on stop. The interval follows the display refresh rate; the log includes report drops so missing samples are visible. Collection is disabled in release builds. `overInterval` is an approximation based on total window duration, not a certified compositor-jank metric. No device samples were collected here. The raw local API probe is saved in `docs/evidence/local-catalog-read.json`; it ran in an uncontrolled development workspace and is reproducibility evidence only.
