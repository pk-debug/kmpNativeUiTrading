/**
 * Owns trading screen state and coordinates orders, search, watchlists and portfolio valuation. Native views observe this model; they do not change balances directly.
 */
package com.pawan.kmpnativeuitrading.features.trading.presentation

import com.pawan.kmpnativeuitrading.features.trading.domain.model.MarketCatalog
import com.pawan.kmpnativeuitrading.features.trading.domain.model.OrderSide
import com.pawan.kmpnativeuitrading.features.trading.domain.model.OrderResult
import com.pawan.kmpnativeuitrading.features.trading.domain.model.TradingLedger
import com.pawan.kmpnativeuitrading.features.trading.domain.model.TradingState
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.TradingRepository
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.OrderExecution
import com.pawan.kmpnativeuitrading.features.trading.domain.usecase.PlaceOrderUseCase
import kotlin.time.Clock
import com.pawan.kmpnativeuitrading.features.trading.data.remote.*
import kotlinx.coroutines.*

interface TradingStateObserver {
    fun onStateChanged(state: TradingState)
}

class TradingViewModel(
    private val repository: TradingRepository,
    currentTimeMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val remote: com.pawan.kmpnativeuitrading.features.trading.domain.repository.AccountGateway? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val placeOrder = PlaceOrderUseCase(repository, currentTimeMillis)
    private val observers = mutableMapOf<Long, TradingStateObserver>()
    private var nextObserverId = 0L
    private var searchText = ""
    private var lastMessage: String? = null
    private var ledger: TradingLedger = if (remote == null) repository.loadLedger() else TradingLedger(0.0, emptyList(), emptyList())
    private var assets = MarketCatalog.assets
    private var indexes = MarketCatalog.indexes
    private var busy = false
    private var synced: Long? = null
    private var valuedLedger: TradingLedger? = null
    private var valuedAssets: List<com.pawan.kmpnativeuitrading.features.trading.domain.model.MarketAsset>? = null
    private var derivedPortfolio = com.pawan.kmpnativeuitrading.features.trading.domain.model.PortfolioSummary(0.0, 0.0, 0.0)

    /** Search and busy messages reuse valuations; only changed holdings/quotes trigger work. */
    private fun updateValuation() {
        if (valuedLedger === ledger && valuedAssets === assets) return
        val prices = assets.associate { it.symbol to it.price }
        val invested = ledger.positions.sumOf { it.averageCost * it.quantity }
        val market = ledger.positions.sumOf { (prices[it.symbol] ?: it.averageCost) * it.quantity }
        derivedPortfolio = com.pawan.kmpnativeuitrading.features.trading.domain.model.PortfolioSummary(market, invested, market - invested)
        valuedLedger = ledger
        valuedAssets = assets
    }

    /** Refresh explicitly: cached-looking prices must never be presented as live quotes. */
    fun refresh() {
        if (remote == null) return
        runRemote {
            assets = remote.assets()
            indexes = remote.indexes()
            applySnapshot(remote.snapshotLedger())
        }
    }

    fun retryOrder() {
        if (remote != null) runRemote {
            applySnapshot(remote.retryOrder())
            lastMessage = "Your paper order is confirmed."
        }
    }

    fun toggleWatchlist(symbol: String) {
        if (assets.none { it.symbol == symbol } || busy) return
        val enabled = symbol !in ledger.watchlist
        if (remote != null) runRemote { applySnapshot(remote.setWatchlist(symbol, enabled)) }
        else {
            val updated = ledger.copy(watchlist = if (enabled) ledger.watchlist + symbol else ledger.watchlist - symbol)
            repository.saveLedger(updated)
            ledger = updated
            publish()
        }
    }

    /** Cancel screen work and remove private balances when the account signs out. */
    fun clearRemoteState() {
        scope.coroutineContext.cancelChildren()
        if (remote != null) ledger = TradingLedger(0.0, emptyList(), emptyList())
        busy = false
        synced = null
        lastMessage = null
        publish()
    }

    private fun applySnapshot(snapshot: TradingLedger) {
        ledger = snapshot
        synced = Clock.System.now().toEpochMilliseconds()
    }

    private fun runRemote(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        publish()
        scope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                lastMessage = (failure as? ApiFailure)?.message
                    ?: "Connection failed. Balances may be stale. Retry any unconfirmed order before placing another."
            } finally { busy = false; publish() }
        }
    }

    val state: TradingState
        get() = createState()

    fun addObserver(observer: TradingStateObserver): Long {
        val id = nextObserverId++
        observers[id] = observer
        observer.onStateChanged(createState())
        return id
    }

    fun close() { scope.cancel(); observers.clear() }

    fun removeObserver(id: Long) {
        observers.remove(id)
    }

    fun setSearchText(value: String) {
        if (searchText == value) return
        searchText = value.take(100)
        publish()
    }

    fun placeOrder(symbol: String, isBuy: Boolean, quantity: Int): OrderResult {
        if (busy) return OrderResult(false, "Please wait for the current request.")
        if (remote != null) {
            if (synced == null) {
                lastMessage = "Refresh the account before placing an order."
                publish()
                return OrderResult(false, checkNotNull(lastMessage))
            }
            if (quantity !in 1..1_000_000 || assets.none { it.symbol == symbol }) return OrderResult(false, "Choose a valid stock and whole-share quantity.")
            if (remote.hasPendingOrder) return OrderResult(false, "Retry the unconfirmed order first.")
            runRemote {
                applySnapshot(remote.submitOrder(symbol, isBuy, quantity))
                lastMessage = "Paper order confirmed by the server."
            }
            return OrderResult(true, "Submitting paper order…")
        }
        return when (val result = placeOrder(symbol, if (isBuy) OrderSide.BUY else OrderSide.SELL, quantity)) {
            is OrderExecution.Accepted -> {
                ledger = result.ledger
                val message = "Demo ${if (isBuy) "buy" else "sale"} completed: $quantity ${if (quantity == 1) "share" else "shares"} of ${symbol.uppercase()}."
                lastMessage = message
                publish()
                OrderResult(accepted = true, message = message)
            }
            is OrderExecution.Rejected -> {
                lastMessage = result.reason
                publish()
                OrderResult(accepted = false, message = result.reason)
            }
        }
    }

    fun clearMessage() {
        lastMessage = null
        publish()
    }

    private fun createState(): TradingState {
        updateValuation()
        return TradingState(
        cash = ledger.cash,
        positions = ledger.positions,
        orders = ledger.orders,
        assets = assets,
        indexes = indexes,
        filteredAssets = assets.filter { it.symbol.contains(searchText.trim(), true) || it.companyName.contains(searchText.trim(), true) },
        searchText = searchText,
        portfolio = derivedPortfolio,
        lastMessage = lastMessage,
        watchlist = ledger.watchlist,
        isBusy = busy,
        isRemote = remote != null,
        canRetryOrder = remote?.hasPendingOrder == true,
        lastSyncedMillis = synced,
    )

    }

    private fun publish() {
        val state = createState()
        observers.values.toList().forEach { it.onStateChanged(state) }
    }
}
