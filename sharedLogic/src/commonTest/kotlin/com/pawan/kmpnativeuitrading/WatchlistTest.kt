package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.data.DefaultTradingRepository
import com.pawan.kmpnativeuitrading.domain.repository.LedgerStorage
import com.pawan.kmpnativeuitrading.presentation.TradingViewModel
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
        model.toggleWatchlist("TCS")
        model.toggleWatchlist("UNKNOWN")
        assertTrue(model.placeOrder("RELIANCE", true, 1).accepted)
        assertEquals(listOf("TCS"), TradingViewModel(repo).state.watchlist)
        model.toggleWatchlist("TCS")
        assertEquals(emptyList(), TradingViewModel(repo).state.watchlist)
    }
}
