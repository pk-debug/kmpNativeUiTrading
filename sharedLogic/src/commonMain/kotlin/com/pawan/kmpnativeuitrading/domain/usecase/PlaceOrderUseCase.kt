package com.pawan.kmpnativeuitrading.domain.usecase

import com.pawan.kmpnativeuitrading.domain.model.MarketCatalog
import com.pawan.kmpnativeuitrading.domain.model.OrderSide
import com.pawan.kmpnativeuitrading.domain.model.Position
import com.pawan.kmpnativeuitrading.domain.model.SimulatedOrder
import com.pawan.kmpnativeuitrading.domain.model.TradingLedger
import com.pawan.kmpnativeuitrading.domain.repository.TradingRepository
import kotlin.time.Clock
import kotlin.math.round
import kotlin.random.Random

class PlaceOrderUseCase(
    private val repository: TradingRepository,
    private val currentTimeMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    operator fun invoke(symbol: String, side: OrderSide, quantity: Int): OrderExecution {
        val asset = MarketCatalog.asset(symbol)
            ?: return OrderExecution.Rejected("That asset is not available in the sample market.")
        if (quantity !in 1..1_000_000) {
            return OrderExecution.Rejected("Enter a whole-share quantity of at least 1.")
        }

        val ledger = repository.loadLedger()
        val positionIndex = ledger.positions.indexOfFirst { (sym) -> sym == asset.symbol }
        // Count paise as whole numbers, like counting coins instead of fractions.
        val pricePaise = round(asset.price * 100).toLong()
        val orderPaise = pricePaise * quantity
        val cashPaise = round(ledger.cash * 100).toLong()
        if ((side == OrderSide.BUY) && (orderPaise > cashPaise)) {
            return OrderExecution.Rejected("There is not enough demo cash for this order.")
        }
        if ((side == OrderSide.SELL) &&
            ((positionIndex < 0) || (quantity > ledger.positions[positionIndex].quantity))
        ) {
            return OrderExecution.Rejected("You do not own enough shares for this demo sale.")
        }

        val order = SimulatedOrder(
            id = Random.nextLong().toString(),
            symbol = asset.symbol,
            side = side,
            quantity = quantity,
            price = asset.price,
            createdAtMillis = currentTimeMillis(),
        )
        val positions = ledger.positions.toMutableList()
        val updatedCash: Double
        if (side == OrderSide.BUY) {
            if (positionIndex >= 0) {
                val previous = positions[positionIndex]
                if (previous.quantity > Int.MAX_VALUE - quantity) return OrderExecution.Rejected("The holding quantity is too large.")
                val updatedQuantity = previous.quantity + quantity
                positions[positionIndex] = previous.copy(
                    quantity = updatedQuantity,
                    averageCost = ((round(previous.averageCost * 100).toLong() * previous.quantity + orderPaise + updatedQuantity / 2) / updatedQuantity) / 100.0,
                )
            } else {
                positions += Position(asset.symbol, quantity, asset.price)
            }
            updatedCash = (cashPaise - orderPaise) / 100.0
        } else {
            val previous = positions[positionIndex]
            val remainingQuantity = previous.quantity - quantity
            if (remainingQuantity == 0) {
                positions.removeAt(positionIndex)
            } else {
                positions[positionIndex] = previous.copy(quantity = remainingQuantity)
            }
            updatedCash = (cashPaise + orderPaise) / 100.0
        }

        val updatedLedger = TradingLedger(
            cash = updatedCash,
            positions = positions,
            orders = listOf(order) + ledger.orders,
            watchlist = ledger.watchlist,
        )
        repository.saveLedger(updatedLedger)
        return OrderExecution.Accepted(updatedLedger)
    }

}

sealed interface OrderExecution {
    data class Accepted(val ledger: TradingLedger) : OrderExecution
    data class Rejected(val reason: String) : OrderExecution
}
