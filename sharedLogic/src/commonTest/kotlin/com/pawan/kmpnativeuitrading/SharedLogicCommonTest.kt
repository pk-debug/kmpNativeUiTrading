package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.data.DefaultTradingRepository
import com.pawan.kmpnativeuitrading.domain.model.OrderSide
import com.pawan.kmpnativeuitrading.domain.model.TradingLedger
import com.pawan.kmpnativeuitrading.domain.repository.LedgerStorage
import com.pawan.kmpnativeuitrading.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.domain.usecase.DemoLoginResult
import com.pawan.kmpnativeuitrading.domain.usecase.DemoLoginUseCase
import com.pawan.kmpnativeuitrading.domain.usecase.CalculatePortfolioUseCase
import com.pawan.kmpnativeuitrading.domain.usecase.OrderExecution
import com.pawan.kmpnativeuitrading.domain.usecase.PlaceOrderUseCase
import com.pawan.kmpnativeuitrading.domain.usecase.SearchAssetsUseCase
import com.pawan.kmpnativeuitrading.presentation.AuthViewModel
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
        val loginViewModel = AuthViewModel(sessionRepository)
        val result = loginViewModel.login("+91 98765 43210", "demo-passphrase")

        assertEquals(true, result.success)
        assertEquals(true, AuthViewModel(sessionRepository).state.isLoggedIn)

        loginViewModel.signOut()

        assertEquals(false, loginViewModel.state.isLoggedIn)
        assertEquals(null, sessionRepository.loadPhoneNumber())
    }

    @Test
    fun buyingSharesUpdatesCashHoldingsAverageCostAndActivity() {
        val repository = DefaultTradingRepository(MemoryLedgerStorage())

        val result = PlaceOrderUseCase(repository) { 123L }("RELIANCE", OrderSide.BUY, 1)

        val accepted = assertIs<OrderExecution.Accepted>(result)
        assertEquals(998_715.50, accepted.ledger.cash)
        assertEquals(9, accepted.ledger.positions.first { it.symbol == "RELIANCE" }.quantity)
        assertEquals(1_218.50, accepted.ledger.positions.first { it.symbol == "RELIANCE" }.averageCost)
        assertEquals(1, accepted.ledger.orders.size)
        assertEquals(123L, accepted.ledger.orders.first().createdAtMillis)
    }

    @Test
    fun sellingAllSharesRemovesHoldingAndCreditsCash() {
        val repository = DefaultTradingRepository(MemoryLedgerStorage())

        val result = PlaceOrderUseCase(repository) { 456L }("HDFCBANK", OrderSide.SELL, 12)

        val accepted = assertIs<OrderExecution.Accepted>(result)
        assertEquals(1_021_220.80, accepted.ledger.cash)
        assertEquals(false, accepted.ledger.positions.any { it.symbol == "HDFCBANK" })
        assertEquals(OrderSide.SELL, accepted.ledger.orders.first().side)
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
        val ledger = repository.loadLedger()
        val summary = CalculatePortfolioUseCase()(ledger.positions)

        assertEquals(listOf("HDFCBANK"), SearchAssetsUseCase()("hdfc").map { it.symbol })
        assertEquals(7, SearchAssetsUseCase()("").size)
        assertEquals(41_945.05, summary.marketValue)
        assertEquals(39_631.00, summary.investedValue)
        assertTrue(abs(summary.gainLoss - 2_314.05) < 0.001)
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
}
