/**
 * Verifies SharedLogicCommon behavior with repeatable assertions. Test fixtures are practice data and do not authorize real trading.
 */
package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.features.trading.data.local.DefaultTradingRepository
import com.pawan.kmpnativeuitrading.features.trading.domain.model.OrderSide
import com.pawan.kmpnativeuitrading.features.trading.domain.model.TradingLedger
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.LedgerStorage
import com.pawan.kmpnativeuitrading.features.auth.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.features.auth.domain.usecase.DemoLoginResult
import com.pawan.kmpnativeuitrading.features.auth.domain.usecase.DemoLoginUseCase
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.CalculatePortfolioUseCase
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.OrderExecution
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.PlaceOrderUseCase
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.SearchAssetsUseCase
import com.pawan.kmpnativeuitrading.features.auth.presentation.AuthViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.math.abs

class SharedLogicCommonTest {
    private class MemoryLedgerStorage : LedgerStorage {
        var value: String? = null
        override fun readLedger(): String? = value
        override fun writeLedger(value: String) {
            this.value = value
        }
    }

    private class MemorySessionRepository : SessionRepository {
        var phoneNumber: String? = null
        override fun loadPhoneNumber(): String? = phoneNumber
        override fun savePhoneNumber(phoneNumber: String) {
            this.phoneNumber = phoneNumber
        }
        override fun clearSession() {
            phoneNumber = null
        }
    }

    @Test
    fun demoLoginValidatesPhoneAndPasswordWithoutSavingPassword() {
        val sessionRepository = MemorySessionRepository()
        val login = DemoLoginUseCase(sessionRepository)

        val invalidPhone = login("12abc", "passphrase")
        val shortPhone = login("123456789", "passphrase")
        val shortPassword = login("+91 98765 43210", "short")
        val success = login(" +91 98765 43210 ", "demo-passphrase")

        assertIs<DemoLoginResult.Rejected>(invalidPhone)
        assertIs<DemoLoginResult.Rejected>(shortPhone)
        assertIs<DemoLoginResult.Rejected>(shortPassword)
        assertEquals(
            DemoLoginResult.Accepted("+91 98765 43210"),
            success,
        )
        assertEquals("+91 98765 43210", sessionRepository.loadPhoneNumber())
    }

    @Test
    fun demoSessionRestoresAndSignOutClearsTheSavedPhone() {
        val sessionRepository = MemorySessionRepository()
        val loginViewModel = AuthViewModel(com.pawan.kmpnativeuitrading.features.auth.data.local.LocalAuthSession(sessionRepository))
        val (success) = loginViewModel.login("+91 98765 43210", "demo-passphrase")

        assertEquals(expected = true, actual = success)
        assertEquals(expected = true, actual = AuthViewModel(com.pawan.kmpnativeuitrading.features.auth.data.local.LocalAuthSession(sessionRepository)).state.isLoggedIn)

        loginViewModel.signOut()

        assertEquals(expected = false, actual = loginViewModel.state.isLoggedIn)
        assertEquals(null, sessionRepository.loadPhoneNumber())
    }

    @Test
    fun buyingSharesUpdatesCashHoldingsAverageCostAndActivity() {
        val repository = DefaultTradingRepository(MemoryLedgerStorage())

        val result = PlaceOrderUseCase(repository) { 123L }("RELIANCE", OrderSide.BUY, 1)

        val (ledger) = assertIs<OrderExecution.Accepted>(result)
        assertEquals(998_715.50, ledger.cash)
        assertEquals(9, ledger.positions.first { (symbol) -> symbol == "RELIANCE" }.quantity)
        assertEquals(1_218.50, ledger.positions.first { (symbol) -> symbol == "RELIANCE" }.averageCost)
        assertEquals(1, ledger.orders.size)
        assertEquals(123L, ledger.orders.first().createdAtMillis)
    }

    @Test
    fun sellingAllSharesRemovesHoldingAndCreditsCash() {
        val repository = DefaultTradingRepository(MemoryLedgerStorage())

        val result = PlaceOrderUseCase(repository) { 456L }("HDFCBANK", OrderSide.SELL, 12)

        val (ledger) = assertIs<OrderExecution.Accepted>(result)
        assertEquals(1_021_220.80, ledger.cash)
        assertEquals(expected = false, actual = ledger.positions.any { (symbol) -> symbol == "HDFCBANK" })
        assertEquals(OrderSide.SELL, ledger.orders.first().side)
    }

    @Test
    fun rejectedOrdersDoNotChangePersistedLedger() {
        val storage = MemoryLedgerStorage()
        val repository = DefaultTradingRepository(storage)
        val initial = repository.loadLedger()
        val useCase = PlaceOrderUseCase(repository) { 789L }

        val insufficientCash = useCase("TCS", OrderSide.BUY, 300)
        val insufficientShares = useCase("TATAMOTORS", OrderSide.SELL, 1)
        val invalidQuantity = useCase("RELIANCE", OrderSide.BUY, 0)
        val unknownAsset = useCase("UNKNOWN", OrderSide.BUY, 1)

        assertIs<OrderExecution.Rejected>(insufficientCash)
        assertIs<OrderExecution.Rejected>(insufficientShares)
        assertIs<OrderExecution.Rejected>(invalidQuantity)
        assertIs<OrderExecution.Rejected>(unknownAsset)
        assertEquals(initial, repository.loadLedger())
    }

    @Test
    fun ledgerSurvivesRepositoryRecreation() {
        val storage = MemoryLedgerStorage()
        val firstRepository = DefaultTradingRepository(storage)
        val result = PlaceOrderUseCase(firstRepository) { 999L }("INFY", OrderSide.BUY, 2)
        val expected = assertIs<OrderExecution.Accepted>(result).ledger

        val reopenedRepository = DefaultTradingRepository(storage)

        assertEquals(expected, reopenedRepository.loadLedger())
    }

    @Test
    fun searchAndPortfolioCalculationsMatchSampleMarket() {
        val repository = DefaultTradingRepository(MemoryLedgerStorage())
        val (_, positions) = repository.loadLedger()
        val (marketValue, investedValue, gainLoss) = CalculatePortfolioUseCase()(positions)

        assertEquals(listOf("HDFCBANK"), SearchAssetsUseCase()("hdfc").map { (symbol) -> symbol })
        assertEquals(7, SearchAssetsUseCase()("").size)
        assertEquals(41_945.05, marketValue)
        assertEquals(39_631.00, investedValue)
        assertTrue(abs(gainLoss - 2_314.05) < 0.001)
    }

    @Test
    fun ledgerSerializerKeepsEmptyHistoryAndCash() {
        val storage = MemoryLedgerStorage()
        val repository = DefaultTradingRepository(storage)
        val initial = repository.loadLedger()

        assertEquals(1_000_000.0, initial.cash)
        assertEquals(emptyList(), initial.orders)
        assertEquals(initial, TradingLedger(initial.cash, initial.positions, emptyList()))
    }
    @Test
    fun localHistoryRetainsOnlyTheLatest500Orders() {
        val repository = DefaultTradingRepository(MemoryLedgerStorage())
        val initial = repository.loadLedger()
        val history = List(500) { index ->
            com.pawan.kmpnativeuitrading.features.trading.domain.model.SimulatedOrder(
                "old-$index", "RELIANCE", OrderSide.BUY, 1, 1284.5, index.toLong(),
            )
        }
        repository.saveLedger(initial.copy(orders = history))
        val result = PlaceOrderUseCase(repository) { 9999L }("RELIANCE", OrderSide.BUY, 1)
        val ledger = assertIs<OrderExecution.Accepted>(result).ledger
        assertEquals(500, ledger.orders.size)
        assertEquals(9999L, ledger.orders.first().createdAtMillis)
        assertEquals("old-498", ledger.orders.last().id)
    }
}
