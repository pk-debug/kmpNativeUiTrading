package com.pawan.kmpnativeuitrading.broker

/** Never choose a real broker by checking whether an API key happens to exist. */
enum class ExecutionMode { PAPER, LIVE }

/** IDs come from the backend; mobile clients must not store a broker secret. */
data class BrokerOrderIntent(
    val clientOrderId: String,
    val symbol: String,
    val quantity: Int,
    val side: String,
)

/** A server-side adapter must implement all these capabilities before live release.
 *
 * A broker accepting an order does not mean the exchange filled it. Reconcile
 * subsequent fills and cancellations against the broker's authoritative ledger.
 */
interface BrokerAdapter {
    val brokerId: String
    suspend fun submit(intent: BrokerOrderIntent): String
    suspend fun cancel(brokerOrderId: String)
    suspend fun reconcile(): List<BrokerOrderStatus>
}

data class BrokerOrderStatus(val brokerOrderId: String, val status: String, val filledQuantity: Int)

/** Fail closed until a separate reviewed broker service exists. No API-key shortcut. */
class ExecutionPolicy {
    fun requireSupported(mode: ExecutionMode) {
        require(mode == ExecutionMode.PAPER) {
            "Live trading is disabled. Complete broker authentication, risk controls, reconciliation and certification first."
        }
    }
}
