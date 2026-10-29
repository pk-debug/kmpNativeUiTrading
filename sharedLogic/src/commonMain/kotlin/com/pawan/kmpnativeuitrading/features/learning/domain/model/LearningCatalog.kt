/** Short educational lessons shared by native UIs; no recommendations or return promises. */
package com.pawan.kmpnativeuitrading.features.learning.domain.model

data class LearningLesson(val id: String, val title: String, val explanation: String)
object LearningCatalog {
    val lessons = listOf(
        LearningLesson("orders", "What does a market order do?", "A market order asks to trade at the available price. In a real market, the execution price can change. This app fills paper market orders using synthetic prices. Limit and stop orders are not supported yet."),
        LearningLesson("risk", "Why does diversification matter?", "Holding several companies and sectors can reduce concentration in one business. It cannot remove market risk or guarantee a profit. Your paper portfolio is practice, not personalized investment advice."),
        LearningLesson("costs", "Why is the order value only an estimate?", "Real trades can include brokerage, taxes and exchange charges. These depend on the broker, instrument and date. Northstar does not deduct these charges or estimate a real broker's fee. Check the provider's official schedule before real trading."),
    )
}
