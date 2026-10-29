/** Owns ActivityScreen presentation responsibilities; shared domain rules remain in Kotlin. */
import SwiftUI
import SharedLogic
import Foundation

struct ActivityScreen: View {
    let state: TradingState

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Activity")
                        .font(.largeTitle.bold())
                        .foregroundStyle(TradingPalette.ink)
                    Text("Your simulated orders, saved on this device")
                        .font(.subheadline)
                        .foregroundStyle(TradingPalette.muted)
                }
                if state.orders.isEmpty {
                    EmptyMessage(text: "Your completed demo orders will appear here.")
                } else {
                    ForEach(state.orders, id: \.id) { order in
                        OrderRow(order: order)
                    }
                }
                DemoDisclaimer()
            }
            .padding(20)
        }
        .background(TradingPalette.canvas.ignoresSafeArea())
        .navigationBarHidden(true)
    }
}
