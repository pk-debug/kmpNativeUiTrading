package com.pawan.kmpnativeuitrading.presentation

import com.pawan.kmpnativeuitrading.domain.model.MarketCatalog
import com.pawan.kmpnativeuitrading.domain.model.OrderSide
import com.pawan.kmpnativeuitrading.domain.model.OrderResult
import com.pawan.kmpnativeuitrading.domain.model.TradingLedger
import com.pawan.kmpnativeuitrading.domain.model.TradingState
import com.pawan.kmpnativeuitrading.domain.repository.TradingRepository
import com.pawan.kmpnativeuitrading.domain.usecase.OrderExecution
import com.pawan.kmpnativeuitrading.domain.usecase.PlaceOrderUseCase
import kotlin.time.Clock
import com.pawan.kmpnativeuitrading.network.*
import kotlinx.coroutines.*

interface TradingStateObserver {
    fun onStateChanged(state: TradingState)
}

class TradingViewModel(
    private val repository: TradingRepository,
    currentTimeMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val remote: RemoteAccount? = null,
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

    /** Refresh explicitly: cached-looking prices must never be presented as live quotes. */
    fun refresh() {
        if (remote == null) return
        runRemote {
            assets = remote.assets()
            indexes = remote.indexes()
            applySnapshot(remote.snapshot())
        }
    }

    fun retryOrder() {
        if (remote != null) runRemote {
            applySnapshot(remote.retry())
            lastMessage = "Your paper order is confirmed."
        }
    }

    fun toggleWatchlist(symbol: String) {
        if (assets.none { it.symbol == symbol } || busy) return
        val enabled = symbol !in ledger.watchlist
        if (remote != null) runRemote { applySnapshot(remote.watch(symbol, enabled)) }
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

    private fun applySnapshot(snapshot: AccountSnapshot) {
        check(snapshot.currency == "INR") { "This app requires an INR account." }
        ledger = snapshot.toLedger()
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
        searchText = value
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
            if (remote.pending != null) return OrderResult(false, "Retry the unconfirmed order first.")
            runRemote {
                applySnapshot(remote.submit(OrderRequest(symbol, if (isBuy) "BUY" else "SELL", quantity)))
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

    private fun createState() = TradingState(
        cash = ledger.cash,
        positions = ledger.positions,
        orders = ledger.orders,
        assets = assets,
        indexes = indexes,
        filteredAssets = assets.filter { it.symbol.contains(searchText.trim(), true) || it.companyName.contains(searchText.trim(), true) },
        searchText = searchText,
        portfolio = com.pawan.kmpnativeuitrading.domain.model.PortfolioSummary(
            marketValue = ledger.positions.sumOf { p -> (assets.find { it.symbol == p.symbol }?.price ?: 0.0) * p.quantity },
            investedValue = ledger.positions.sumOf { it.averageCost * it.quantity },
            gainLoss = ledger.positions.sumOf { p -> ((assets.find { it.symbol == p.symbol }?.price ?: p.averageCost) - p.averageCost) * p.quantity },
        ),
        lastMessage = lastMessage,
        watchlist = ledger.watchlist,
        isBusy = busy,
        isRemote = remote != null,
        canRetryOrder = remote?.pending != null,
        lastSyncedMillis = synced,
    )

    private fun publish() {
        val state = createState()
        observers.values.toList().forEach { it.onStateChanged(state) }
    }
}
