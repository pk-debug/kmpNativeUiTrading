/** Defines the trading feature's server boundary using domain values only. */
package com.pawan.kmpnativeuitrading.features.trading.domain.repository

import com.pawan.kmpnativeuitrading.features.trading.domain.model.*

interface AccountGateway {
    val hasPendingOrder: Boolean
    suspend fun assets(): List<MarketAsset>
    suspend fun indexes(): List<MarketIndex>
    suspend fun snapshotLedger(): TradingLedger
    suspend fun submitOrder(symbol: String, isBuy: Boolean, quantity: Int): TradingLedger
    suspend fun retryOrder(): TradingLedger
    suspend fun setWatchlist(symbol: String, enabled: Boolean): TradingLedger
}
