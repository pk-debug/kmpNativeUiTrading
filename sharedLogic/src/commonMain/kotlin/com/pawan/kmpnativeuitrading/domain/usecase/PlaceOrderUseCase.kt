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
        if (quantity < 1) {
            return OrderExecution.Rejected("Enter a whole-share quantity of at least 1.")
        }

        val ledger = repository.loadLedger()
        val positionIndex = ledger.positions.indexOfFirst { it.symbol == asset.symbol }
        val orderValue = roundToCents(asset.price * quantity)
        if (side == OrderSide.BUY && orderValue > ledger.cash) {
            return OrderExecution.Rejected("There is not enough demo cash for this order.")
        }
        if (side == OrderSide.SELL &&
            (positionIndex < 0 || quantity > ledger.positions[positionIndex].quantity)
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
                val updatedQuantity = previous.quantity + quantity
                positions[positionIndex] = previous.copy(
                    quantity = updatedQuantity,
                    averageCost = (
                        previous.averageCost * previous.quantity + orderValue
                        ) / updatedQuantity,
                )
            } else {
                positions += Position(asset.symbol, quantity, asset.price)
            }
            updatedCash = roundToCents(ledger.cash - orderValue)
        } else {
            val previous = positions[positionIndex]
            val remainingQuantity = previous.quantity - quantity
            if (remainingQuantity == 0) {
                positions.removeAt(positionIndex)
            } else {
                positions[positionIndex] = previous.copy(quantity = remainingQuantity)
            }
            updatedCash = roundToCents(ledger.cash + orderValue)
        }

        val updatedLedger = TradingLedger(
            cash = updatedCash,
            positions = positions,
            orders = listOf(order) + ledger.orders,
        )
        repository.saveLedger(updatedLedger)
        return OrderExecution.Accepted(updatedLedger)
    }

    private fun roundToCents(amount: Double): Double = round(amount * 100) / 100
}

sealed interface OrderExecution {
    data class Accepted(val ledger: TradingLedger) : OrderExecution
    data class Rejected(val reason: String) : OrderExecution
}
