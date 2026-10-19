package com.pawan.kmpnativeuitrading.presentation

import com.pawan.kmpnativeuitrading.domain.model.MarketCatalog
import com.pawan.kmpnativeuitrading.domain.model.OrderSide
import com.pawan.kmpnativeuitrading.domain.model.OrderResult
import com.pawan.kmpnativeuitrading.domain.model.TradingLedger
import com.pawan.kmpnativeuitrading.domain.model.TradingState
import com.pawan.kmpnativeuitrading.domain.repository.TradingRepository
import com.pawan.kmpnativeuitrading.domain.usecase.CalculatePortfolioUseCase
import com.pawan.kmpnativeuitrading.domain.usecase.OrderExecution
import com.pawan.kmpnativeuitrading.domain.usecase.PlaceOrderUseCase
import com.pawan.kmpnativeuitrading.domain.usecase.SearchAssetsUseCase
import kotlin.time.Clock

interface TradingStateObserver {
    fun onStateChanged(state: TradingState)
}

class TradingViewModel(
    private val repository: TradingRepository,
    currentTimeMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val searchAssets = SearchAssetsUseCase()
    private val calculatePortfolio = CalculatePortfolioUseCase()
    private val placeOrder = PlaceOrderUseCase(repository, currentTimeMillis)
    private val observers = mutableMapOf<Long, TradingStateObserver>()
    private var nextObserverId = 0L
    private var searchText = ""
    private var lastMessage: String? = null
    private var ledger: TradingLedger = repository.loadLedger()

    val state: TradingState
        get() = createState()

    fun addObserver(observer: TradingStateObserver): Long {
        val id = nextObserverId++
        observers[id] = observer
        observer.onStateChanged(createState())
        return id
    }

    fun removeObserver(id: Long) {
        observers.remove(id)
    }

    fun setSearchText(value: String) {
        searchText = value
        publish()
    }

    fun placeOrder(symbol: String, isBuy: Boolean, quantity: Int): OrderResult {
        return when (val result = placeOrder(symbol, if (isBuy) OrderSide.BUY else OrderSide.SELL, quantity)) {
            is OrderExecution.Accepted -> {
                ledger = result.ledger
                val message = "Demo ${if (isBuy) "buy" else "sale"} completed: $quantity ${if (quantity == 1) "share" else "shares"} of ${symbol.uppercase()}."
                lastMessage = message
                publish()
                OrderResult(true, message)
            }
            is OrderExecution.Rejected -> {
                lastMessage = result.reason
                publish()
                OrderResult(false, result.reason)
            }
        }
    }

    fun clearMessage() {
        lastMessage = null
        publish()
    }

    private fun createState() = TradingState(
        cash = ledger.cash,
        positions = ledger.positions,
        orders = ledger.orders,
        assets = MarketCatalog.assets,
        indexes = MarketCatalog.indexes,
        filteredAssets = searchAssets(searchText),
        searchText = searchText,
        portfolio = calculatePortfolio(ledger.positions),
        lastMessage = lastMessage,
    )

    private fun publish() {
        val state = createState()
        observers.values.toList().forEach { it.onStateChanged(state) }
    }
}
