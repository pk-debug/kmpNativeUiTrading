package com.pawan.kmpnativeuitrading.domain.usecase

import com.pawan.kmpnativeuitrading.domain.model.MarketCatalog
import com.pawan.kmpnativeuitrading.domain.model.PortfolioSummary
import com.pawan.kmpnativeuitrading.domain.model.Position

class CalculatePortfolioUseCase {
    operator fun invoke(positions: List<Position>): PortfolioSummary {
        val marketValue = positions.sumOf { (symbol, quantity) ->
            (MarketCatalog.asset(symbol)?.price ?: 0.0) * quantity
        }
        val investedValue = positions.sumOf { (_, quantity, averageCost) -> averageCost * quantity }
        return PortfolioSummary(
            marketValue = marketValue,
            investedValue = investedValue,
            gainLoss = marketValue - investedValue,
        )
    }
}
