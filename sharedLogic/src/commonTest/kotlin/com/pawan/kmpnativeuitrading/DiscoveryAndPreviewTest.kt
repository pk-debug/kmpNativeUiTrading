/** Exercises discovery combinations and exact-paise order estimates before any execution. */
package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.features.trading.domain.model.MarketCatalog
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.DiscoverAssetsUseCase
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.PreviewOrderUseCase
import kotlin.test.*

class DiscoveryAndPreviewTest {
    private val discover = DiscoverAssetsUseCase()
    private val preview = PreviewOrderUseCase()
    private val asset = MarketCatalog.assets.first { it.symbol == "RELIANCE" }

    @Test fun combinesCaseInsensitiveQueryAndSector() {
        val result = discover(MarketCatalog.assets, " tCs ", "Information Technology", "NAME")
        assertEquals(listOf("TCS"), result.map { it.symbol })
        assertEquals(emptyList(), discover(MarketCatalog.assets, "tcs", "Banking", "NAME"))
    }
    @Test fun ranksMoversAndBreaksTiesBySymbolWithoutChangingCatalog() {
        val before = MarketCatalog.assets.toList()
        assertEquals("TATAMOTORS", discover(before, "", "", "GAINERS").first().symbol)
        assertEquals("ITC", discover(before, "", "", "LOSERS").first().symbol)
        val tied = listOf(asset.copy(symbol = "ZZZ", changePercent = 1.0), asset.copy(symbol = "AAA", changePercent = 1.0))
        assertEquals(listOf("AAA", "ZZZ"), discover(tied, "", "", "GAINERS").map { it.symbol })
        assertEquals(listOf("ZZZ", "AAA"), tied.map { it.symbol })
        assertEquals(before, MarketCatalog.assets)
    }
    @Test fun buyEstimateChecksCashAndMaximumQuantity() {
        val result = preview(asset, true, 1, 1330.0, 0)
        assertTrue(result.canSubmit)
        assertEquals(1284.5, result.estimatedValue)
        assertEquals(45.5, result.cashAfter)
        assertEquals(1, result.maxBuyQuantity)
        assertFalse(preview(asset, true, 2, 1330.0, 0).canSubmit)
    }
    @Test fun saleEstimateChecksOwnedSharesAndCreditsCash() {
        assertFalse(preview(asset, false, 3, 100.0, 2).canSubmit)
        val result = preview(asset, false, 2, 100.0, 2)
        assertTrue(result.canSubmit)
        assertEquals(2669.0, result.cashAfter)
        assertEquals(2, result.ownedQuantity)
    }
    @Test fun rejectsInvalidQuantityOrUnavailableQuote() {
        for(q in listOf(0, -1, 1_000_001, Int.MAX_VALUE)) assertFalse(preview(asset, true, q, 2000.0, 0).canSubmit)
        for(price in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0, 0.001)) {
            assertFalse(preview(asset.copy(price = price), true, 1, 2000.0, 0).canSubmit)
        }
        assertFalse(preview(asset, true, 1, Double.NaN, 0).canSubmit)
    }
    @Test fun estimatesFractionalRupeesUsingWholePaise() {
        val result = preview(asset.copy(price = 0.1), true, 3, 0.3, 0)
        assertTrue(result.canSubmit)
        assertEquals(0.3, result.estimatedValue)
        assertEquals(0.0, result.cashAfter)
    }
}
