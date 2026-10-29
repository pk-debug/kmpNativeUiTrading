/** Executes offline practice rules through composition; no screen code owns persistence. */
package com.pawan.kmpnativeuitrading.features.trading.data.local
import com.pawan.kmpnativeuitrading.core.domain.*
import com.pawan.kmpnativeuitrading.features.trading.domain.model.*
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.*
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.*
import kotlin.time.Clock

class LocalTradingSession(private val repository: TradingRepository, clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }) : TradingSession {
    private val place = PlaceOrderUseCase(repository, clock)
    override val initialLedger = repository.loadLedger()
    override val isRemote = false
    override val hasPendingOrder = false
    override suspend fun refresh() = TradingSnapshot(repository.loadLedger(), MarketCatalog.assets, MarketCatalog.indexes)
    override suspend fun submit(symbol: String, isBuy: Boolean, quantity: Int): TradingLedger = when(val result = place(symbol, if(isBuy) OrderSide.BUY else OrderSide.SELL, quantity)) {
        is OrderExecution.Accepted -> result.ledger
        is OrderExecution.Rejected -> throw AccountFailure(FailureKind.INVALID_INPUT, result.reason)
    }
    override suspend fun retry(): TradingLedger = throw AccountFailure(FailureKind.INVALID_INPUT, "No unconfirmed order exists.")
    override suspend fun watch(symbol: String, enabled: Boolean): TradingLedger {
        val ledger = repository.loadLedger()
        val next = ledger.copy(watchlist = if(enabled) (ledger.watchlist + symbol).distinct() else ledger.watchlist - symbol)
        repository.saveLedger(next)
        return next
    }
    override fun reset() = repository.loadLedger()
}
