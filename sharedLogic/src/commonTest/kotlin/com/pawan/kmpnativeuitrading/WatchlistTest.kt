/**
 * Verifies Watchlist behavior with repeatable assertions. Test fixtures are practice data and do not authorize real trading.
 */
package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.features.trading.data.local.DefaultTradingRepository
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.LedgerStorage
import com.pawan.kmpnativeuitrading.features.trading.presentation.TradingViewModel
import kotlin.test.*

class WatchlistTest {
    @Test fun watchlistSurvivesOrdersAndViewModelRecreation() {
        val storage = object : LedgerStorage {
            var data: String? = null
            override fun readLedger() = data
            override fun writeLedger(value: String) { data = value }
        }
        val repo = DefaultTradingRepository(storage)
        val model = TradingViewModel(repo)
        val valuation = model.state.portfolio
        model.setSearchText("TCS")
        assertSame(valuation, model.state.portfolio, "Search must reuse the portfolio valuation")
        model.toggleWatchlist("TCS")
        model.toggleWatchlist("UNKNOWN")
        assertTrue(model.placeOrder("RELIANCE", true, 1).accepted)
        assertEquals(listOf("TCS"), TradingViewModel(repo).state.watchlist)
        model.toggleWatchlist("TCS")
        assertEquals(emptyList(), TradingViewModel(repo).state.watchlist)
    }
}
