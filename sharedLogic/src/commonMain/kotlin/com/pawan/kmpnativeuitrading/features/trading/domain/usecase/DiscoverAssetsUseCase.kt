/** Filters and deterministically ranks quotes without mutating the source catalog. */
package com.pawan.kmpnativeuitrading.features.trading.domain.usecase
import com.pawan.kmpnativeuitrading.features.trading.domain.model.MarketAsset

class DiscoverAssetsUseCase {
    operator fun invoke(assets: List<MarketAsset>, query: String, category: String, sort: String): List<MarketAsset> {
        val needle = query.trim()
        val selected = assets.filter { (category.isEmpty() || it.category == category) &&
            (it.symbol.contains(needle, true) || it.companyName.contains(needle, true)) }
        return when(sort) {
            "GAINERS" -> selected.sortedWith(compareByDescending<MarketAsset> { it.changePercent }.thenBy { it.symbol })
            "LOSERS" -> selected.sortedWith(compareBy<MarketAsset> { it.changePercent }.thenBy { it.symbol })
            else -> selected.sortedBy { it.symbol }
        }
    }
}
