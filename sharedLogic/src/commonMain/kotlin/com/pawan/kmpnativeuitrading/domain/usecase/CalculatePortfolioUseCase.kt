package com.pawan.kmpnativeuitrading.domain.usecase

import com.pawan.kmpnativeuitrading.domain.model.MarketCatalog
import com.pawan.kmpnativeuitrading.domain.model.PortfolioSummary
import com.pawan.kmpnativeuitrading.domain.model.Position

class CalculatePortfolioUseCase {
    operator fun invoke(positions: List<Position>): PortfolioSummary {
        val marketValue = positions.sumOf { position ->
            (MarketCatalog.asset(position.symbol)?.price ?: 0.0) * position.quantity
        }
        val investedValue = positions.sumOf { it.averageCost * it.quantity }
        return PortfolioSummary(
            marketValue = marketValue,
            investedValue = investedValue,
            gainLoss = marketValue - investedValue,
        )
    }
}
