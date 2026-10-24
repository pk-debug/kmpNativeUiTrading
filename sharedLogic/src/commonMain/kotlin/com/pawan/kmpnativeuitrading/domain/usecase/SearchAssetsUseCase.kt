package com.pawan.kmpnativeuitrading.domain.usecase

import com.pawan.kmpnativeuitrading.domain.model.MarketAsset
import com.pawan.kmpnativeuitrading.domain.model.MarketCatalog

class SearchAssetsUseCase {
    operator fun invoke(query: String): List<MarketAsset> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return MarketCatalog.assets
        return MarketCatalog.assets.filter { (symbol, companyName) ->
            symbol.contains(normalizedQuery, ignoreCase = true) ||
                companyName.contains(normalizedQuery, ignoreCase = true)
        }
    }
}
