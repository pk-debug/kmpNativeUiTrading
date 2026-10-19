package com.pawan.kmpnativeuitrading.domain.model

import kotlinx.serialization.Serializable

data class MarketAsset(
    val symbol: String,
    val companyName: String,
    val exchange: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val history: List<Double>,
    val category: String,
)

data class MarketIndex(
    val name: String,
    val value: String,
    val change: String,
    val isPositive: Boolean,
)

@Serializable
data class Position(
    val symbol: String,
    val quantity: Int,
    val averageCost: Double,
)

@Serializable
enum class OrderSide {
    BUY,
    SELL,
}

@Serializable
data class SimulatedOrder(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val quantity: Int,
    val price: Double,
    val createdAtMillis: Long,
) {
    val total: Double
        get() = price * quantity
}

@Serializable
data class TradingLedger(
    val cash: Double,
    val positions: List<Position>,
    val orders: List<SimulatedOrder>,
)

data class PortfolioSummary(
    val marketValue: Double,
    val investedValue: Double,
    val gainLoss: Double,
)

data class TradingState(
    val cash: Double,
    val positions: List<Position>,
    val orders: List<SimulatedOrder>,
    val assets: List<MarketAsset>,
    val indexes: List<MarketIndex>,
    val filteredAssets: List<MarketAsset>,
    val searchText: String,
    val portfolio: PortfolioSummary,
    val lastMessage: String?,
)

data class OrderResult(
    val accepted: Boolean,
    val message: String,
)
