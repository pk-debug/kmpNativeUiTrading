# Security design and limits

This is paper trading. Security controls reduce common risks; they do not establish that a system is unhackable or compliant with a financial regulator.

| Threat | Implemented control | Remaining responsibility |
|---|---|---|
| User changes another account's balance | Server derives account from bearer session; all private queries scope by user | Independent security review and broader adversarial tests |
| Duplicate taps / lost response | Busy guard, persisted UUID, unique user/key constraint and atomic transaction | Real broker reconciliation before live execution |
| Overspending / overselling | Write lock before balance read; server fixture price; integer-paise math | Real margin, fees and execution risk engine |
| Stolen password table | Unique random salt, scrypt n=16384/r=8/p=1, constant-time comparison | Tune hash cost and identity recovery; protect backups |
| Guessed credentials | Generic login errors, database-backed account/IP throttle | Gateway limits, capacity testing and managed identity for a larger deployment |
| Stolen token database | Server stores SHA-256 digests, not raw bearer tokens | Host access controls and session incident response |
| Token leakage from phone backups | Tokens only in memory; no password persistence; Android backups disabled | Session ends on process death; biometric quick-unlock is not implemented |
| Transport interception | TLS verification, no cross-host redirects, HTTPS required for release | Certificate lifecycle and correct ingress; no pinning claim |
| Ambiguous order status | Explicit retry UI, original UUID survives restart | No silent offline trade queue |
| Privacy leaks in diagnostics | Safe error messages, no request-body logging, no analytics/ad SDK | Redacted production monitoring and privacy-policy URLs |
| Accidental live trading | ExecutionPolicy rejects LIVE | Reviewed broker adapter and release approval |

## Why no hardcoded certificate pin?

A pin without rotation and backup pins can lock every installed app out during a certificate change. This app uses the platform trust store and HTTPS. Pinning may be added with a tested rotation plan; it is not falsely advertised as implemented. No TLS or artifact integrity checks were disabled to build this project.

## Why no Keystore token file?

There is no persistent token file to protect. Memory-only bearer sessions intentionally require login after process death. If persistent sessions are introduced, use an audited Android Keystore encryption boundary and iOS Keychain with a documented accessibility class, expiry, revocation and migration behavior. Storing a placeholder token in SharedPreferences and calling it secure storage would not demonstrate those skills.

## Public deployment gate

Before making the service public: finish the hosting TLS/ingress configuration, dependency vulnerability review, independent auth/authorization review, backup restoration exercise, load/concurrency validation, abuse protections, real privacy/support/deletion URLs and incident response. No password recovery, MFA, push notifications, Play Integrity attestation, jailbreak/root detection, fraud engine, or regulated KYC is implemented. Attestation can be defense in depth later; it must not replace server-side authorization.

Offline demo data belongs to the device owner and is editable on a compromised device. It is never promoted to an API account or a real broker ledger. API mode starts with the server's own account state. Network snapshots are read-only when disconnected. A foreground refresh does not authorize background trading.
