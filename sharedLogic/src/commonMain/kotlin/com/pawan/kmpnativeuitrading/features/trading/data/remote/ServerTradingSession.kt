/** Adapts a server account to the same trading-session contract as offline practice. */
package com.pawan.kmpnativeuitrading.features.trading.data.remote
import com.pawan.kmpnativeuitrading.core.domain.*
import com.pawan.kmpnativeuitrading.features.trading.domain.model.*
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.*

class ServerTradingSession(private val gateway: AccountGateway) : TradingSession {
    private var refreshed = false
    override val initialLedger = TradingLedger(0.0, emptyList(), emptyList())
    override val isRemote = true
    override val hasPendingOrder get() = gateway.hasPendingOrder
    override suspend fun refresh(): TradingSnapshot {
        val assets = gateway.assets()
        val indexes = gateway.indexes()
        val ledger = gateway.snapshotLedger()
        refreshed = true
        return TradingSnapshot(ledger, assets, indexes)
    }
    override suspend fun submit(symbol: String, isBuy: Boolean, quantity: Int): TradingLedger {
        if(!refreshed) throw AccountFailure(FailureKind.INVALID_INPUT, "Refresh the account before placing an order.")
        if(hasPendingOrder) throw AccountFailure(FailureKind.CONFLICT, "Retry the unconfirmed order first.")
        return gateway.submitOrder(symbol, isBuy, quantity)
    }
    override suspend fun retry() = gateway.retryOrder()
    override suspend fun watch(symbol: String, enabled: Boolean) = gateway.setWatchlist(symbol, enabled)
    override fun reset(): TradingLedger { refreshed = false; return initialLedger }
}
