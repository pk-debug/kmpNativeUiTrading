/** Verifies behavior shared by local and server session adapters, using domain-only fake gateways. */
package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.core.domain.*
import com.pawan.kmpnativeuitrading.features.trading.data.local.*
import com.pawan.kmpnativeuitrading.features.trading.data.remote.ServerTradingSession
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.*
import com.pawan.kmpnativeuitrading.features.trading.domain.model.*
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.CalculatePortfolioUseCase
import com.pawan.kmpnativeuitrading.features.auth.data.local.LocalAuthSession
import com.pawan.kmpnativeuitrading.features.auth.data.remote.ServerAuthSession
import com.pawan.kmpnativeuitrading.features.auth.domain.repository.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SessionContractTest {
    private fun local(): TradingSession {
        val storage = object : LedgerStorage {
            var value: String? = null
            override fun readLedger() = value
            override fun writeLedger(value: String) { this.value = value }
        }
        return LocalTradingSession(DefaultTradingRepository(storage))
    }
    private fun server(): TradingSession {
        val delegate = local()
        return ServerTradingSession(object : AccountGateway {
            override val hasPendingOrder = false
            override suspend fun assets() = MarketCatalog.assets
            override suspend fun indexes() = MarketCatalog.indexes
            override suspend fun snapshotLedger() = delegate.refresh().ledger
            override suspend fun submitOrder(symbol: String, isBuy: Boolean, quantity: Int) = delegate.submit(symbol, isBuy, quantity)
            override suspend fun retryOrder() = delegate.retry()
            override suspend fun setWatchlist(symbol: String, enabled: Boolean) = delegate.watch(symbol, enabled)
        })
    }

    @Test fun tradingAdaptersPreserveMoneyWatchlistAndRejectionContracts() = runTest {
        for (session in listOf(local(), server())) {
            // The common contract requires refresh before account-changing commands.
            val before = session.refresh().ledger
            val bought = session.submit("RELIANCE", true, 1)
            assertEquals(before.cash - 1284.5, bought.cash)
            session.watch("TCS", true)
            session.watch("TCS", true)
            assertEquals(listOf("TCS"), session.refresh().ledger.watchlist)
            val sold = session.submit("RELIANCE", false, 1)
            assertEquals(before.cash, sold.cash)
            val stable = session.refresh().ledger
            assertFailsWith<AccountFailure> { session.submit("RELIANCE", true, 1_000_000) }
            assertEquals(stable, session.refresh().ledger)
        }
    }

    @Test fun missingQuotesUseCostConsistently() {
        val summary = CalculatePortfolioUseCase()(listOf(Position("UNKNOWN", 2, 100.0)), emptyList())
        assertEquals(200.0, summary.marketValue)
        assertEquals(200.0, summary.investedValue)
        assertEquals(0.0, summary.gainLoss)
    }

    @Test fun authAdaptersShareLoginAndSignOutContracts() = runTest {
        val repository = object : SessionRepository {
            var identity: String? = null
            override fun loadPhoneNumber() = identity
            override fun savePhoneNumber(phoneNumber: String) { identity = phoneNumber }
            override fun clearSession() { identity = null }
        }
        var cleared = false
        val remote = ServerAuthSession(object : AuthGateway {
            override suspend fun authenticate(name: String, password: String, register: Boolean) = Unit
            override suspend fun revokeSession() = Unit
            override suspend fun deleteAccount() = Unit
            override fun clearSession() { cleared = true }
        })
        for (session in listOf(LocalAuthSession(repository), remote)) {
            assertEquals("0000000000", session.login("0000000000", "demo-passphrase", false))
            assertNull(session.signOut())
            assertFailsWith<AccountFailure> { session.login("x", "short", false) }
        }
        assertTrue(cleared)
        assertNull(repository.identity)
    }
}
