/** Encapsulates execution mode so screen state never chooses storage versus HTTP. */
package com.pawan.kmpnativeuitrading.features.trading.domain.repository
import com.pawan.kmpnativeuitrading.features.trading.domain.model.*

data class TradingSnapshot(val ledger: TradingLedger, val assets: List<MarketAsset>, val indexes: List<MarketIndex>)
interface TradingSession {
    val initialLedger: TradingLedger
    val isRemote: Boolean
    val hasPendingOrder: Boolean
    suspend fun refresh(): TradingSnapshot
    suspend fun submit(symbol: String, isBuy: Boolean, quantity: Int): TradingLedger
    suspend fun retry(): TradingLedger
    suspend fun watch(symbol: String, enabled: Boolean): TradingLedger
    fun reset(): TradingLedger
}
