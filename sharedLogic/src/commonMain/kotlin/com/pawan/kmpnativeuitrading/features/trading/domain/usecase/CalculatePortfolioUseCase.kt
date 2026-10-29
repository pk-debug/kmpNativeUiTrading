/**
 * Defines CalculatePortfolioUseCase for the shared native application. Keep platform UI concerns outside domain rules.
 */
package com.pawan.kmpnativeuitrading.features.trading.domain.usecase

import com.pawan.kmpnativeuitrading.features.trading.domain.model.MarketCatalog
import com.pawan.kmpnativeuitrading.features.trading.domain.model.MarketAsset
import com.pawan.kmpnativeuitrading.features.trading.domain.model.PortfolioSummary
import com.pawan.kmpnativeuitrading.features.trading.domain.model.Position

class CalculatePortfolioUseCase {
    operator fun invoke(positions: List<Position>, assets: List<MarketAsset> = MarketCatalog.assets): PortfolioSummary {
        // A missing quote is valued at cost, avoiding a fabricated total loss.
        val prices = assets.associate { it.symbol to it.price }
        val marketValue = positions.sumOf { (symbol, quantity, cost) -> (prices[symbol] ?: cost) * quantity }
        val investedValue = positions.sumOf { (_, quantity, averageCost) -> averageCost * quantity }
        return PortfolioSummary(
            marketValue = marketValue,
            investedValue = investedValue,
            gainLoss = marketValue - investedValue,
        )
    }
}
