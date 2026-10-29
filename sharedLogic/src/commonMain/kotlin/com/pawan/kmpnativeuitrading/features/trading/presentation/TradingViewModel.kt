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
import com.pawan.kmpnativeuitrading.core.domain.AccountFailure
import kotlinx.coroutines.*

interface TradingStateObserver {
    fun onStateChanged(state: TradingState)
}

class TradingViewModel(
    private val session: com.pawan.kmpnativeuitrading.features.trading.domain.repository.TradingSession,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val calculatePortfolio = com.pawan.kmpnativeuitrading.features.trading.domain.usecase.CalculatePortfolioUseCase()
    private val observers = mutableMapOf<Long, TradingStateObserver>()
    private var nextObserverId = 0L
    private var searchText = ""
    private var lastMessage: String? = null
    private var ledger: TradingLedger = session.initialLedger
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
        derivedPortfolio = calculatePortfolio(ledger.positions, assets)
        valuedLedger = ledger
        valuedAssets = assets
    }

    /** Refresh explicitly: cached-looking prices must never be presented as live quotes. */
    fun refresh() = runOperation {
        val snapshot = session.refresh()
        assets = snapshot.assets
        indexes = snapshot.indexes
        applySnapshot(snapshot.ledger)
    }

    fun retryOrder() = runOperation {
        applySnapshot(session.retry())
        lastMessage = "Your paper order is confirmed."
    }

    fun toggleWatchlist(symbol: String) {
        if (assets.none { it.symbol == symbol } || busy) return
        runOperation { applySnapshot(session.watch(symbol, symbol !in ledger.watchlist)) }
    }

    /** Cancel screen work and remove private balances when the account signs out. */
    fun clearRemoteState() {
        scope.coroutineContext.cancelChildren()
        ledger = session.reset()
        busy = false
        synced = null
        lastMessage = null
        publish()
    }

    private fun applySnapshot(snapshot: TradingLedger) {
        ledger = snapshot
        synced = Clock.System.now().toEpochMilliseconds()
    }

    private fun runOperation(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        publish()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                lastMessage = (failure as? AccountFailure)?.message
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
        if (quantity !in 1..1_000_000 || assets.none { it.symbol == symbol }) return OrderResult(false, "Choose a valid stock and whole-share quantity.")
        var result = OrderResult(true, "Submitting paper order…")
        busy = true
        publish()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                applySnapshot(session.submit(symbol, isBuy, quantity))
                lastMessage = "Paper order confirmed."
                result = OrderResult(true, checkNotNull(lastMessage))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                lastMessage = (failure as? AccountFailure)?.message ?: "Connection failed. Retry any unconfirmed order before placing another."
                result = OrderResult(false, checkNotNull(lastMessage))
            } finally { busy = false; publish() }
        }
        return result
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
        isRemote = session.isRemote,
        canRetryOrder = session.hasPendingOrder,
        lastSyncedMillis = synced,
    )

    }

    private fun publish() {
        val state = createState()
        observers.values.toList().forEach { it.onStateChanged(state) }
    }
}
