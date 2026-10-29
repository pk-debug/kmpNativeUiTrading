/** Owns PortfolioScreen presentation responsibilities; shared domain rules remain in Kotlin. */
import SwiftUI
import SharedLogic
import Foundation

struct PortfolioScreen: View {
    @ObservedObject var model: TradingObservableModel
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Portfolio")
                        .font(.largeTitle.bold())
                        .foregroundStyle(TradingPalette.ink)
                    Text(model.state.isRemote ? "Server-confirmed practice holdings" : "Your locally saved practice holdings")
                        .font(.subheadline)
                        .foregroundStyle(TradingPalette.muted)
                }

                AccountSummaryCard(
                    title: "Invested in stocks",
                    value: formatMoney(model.state.portfolio.marketValue),
                    subtitle: "Cash \(formatMoney(model.state.cash))"
                )

                HStack {
                    SummaryValue(
                        title: "Total account",
                        value: formatMoney(model.state.cash + model.state.portfolio.marketValue)
                    )
                    Spacer()
                    SummaryValue(
                        title: "Unrealized P/L",
                        value: formatMoney(model.state.portfolio.gainLoss),
                        positive: model.state.portfolio.gainLoss >= 0
                    )
                }
                .padding(18)
                .background(.white, in: RoundedRectangle(cornerRadius: 18))

                Text("Holdings")
                    .font(.title3.bold())
                    .foregroundStyle(TradingPalette.ink)
                if model.state.positions.isEmpty {
                    EmptyMessage(text: "No holdings yet. Explore stocks to simulate a buy.")
                } else {
                    ForEach(model.state.positions, id: \.symbol) { position in
                        if let asset = model.state.assets.first(where: { $0.symbol == position.symbol }) {
                            PositionRow(position: position, asset: asset)
                                .contentShape(Rectangle())
                                .onTapGesture { onSelect(asset.symbol) }
                        }
                    }
                }
                DemoDisclaimer()
            }
            .padding(20)
        }
        .background(TradingPalette.canvas.ignoresSafeArea())
        .navigationTitle("")
        .navigationBarHidden(true)
    }
}
