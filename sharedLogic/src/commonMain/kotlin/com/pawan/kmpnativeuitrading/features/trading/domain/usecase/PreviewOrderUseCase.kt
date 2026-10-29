/** Explains affordability before review; the server still authorizes and prices API orders. */
package com.pawan.kmpnativeuitrading.features.trading.domain.usecase
import com.pawan.kmpnativeuitrading.features.trading.domain.model.MarketAsset
import kotlin.math.round

data class OrderPreview(val estimatedValue: Double, val cashAfter: Double, val maxBuyQuantity: Int, val ownedQuantity: Int, val error: String?) {
    val canSubmit: Boolean get() = error == null
}
class PreviewOrderUseCase {
    operator fun invoke(asset: MarketAsset, isBuy: Boolean, quantity: Int, cash: Double, owned: Int): OrderPreview {
        if (!asset.price.isFinite() || asset.price <= 0 || asset.price > 1_000_000_000 || !cash.isFinite() || cash < 0 || cash > 1_000_000_000_000)
            return OrderPreview(0.0, 0.0, 0, owned, "Account or quote data is unavailable. Refresh before trading.")
        val price = round(asset.price * 100).toLong()
        if (price <= 0) return OrderPreview(0.0, cash, 0, owned, "The quote must be at least one paisa.")
        val balance = round(cash * 100).toLong()
        val max = (balance / price).coerceAtMost(1_000_000).toInt()
        if(quantity !in 1..1_000_000) return OrderPreview(0.0, cash, max, owned, "Enter 1–1,000,000 whole shares.")
        val total = price * quantity
        val error = when {
            isBuy && total > balance -> "Not enough practice cash. You can buy up to $max shares."
            !isBuy && quantity > owned -> "Not enough shares. You own $owned shares."
            else -> null
        }
        return OrderPreview(total / 100.0, (if(isBuy) balance - total else balance + total) / 100.0, max, owned, error)
    }
}
