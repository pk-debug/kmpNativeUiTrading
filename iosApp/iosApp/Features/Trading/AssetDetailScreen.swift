/** Owns AssetDetailScreen presentation responsibilities; shared domain rules remain in Kotlin. */
import SwiftUI
import SharedLogic
import Foundation

struct AssetDetailScreen: View {
    @Environment(\.dismiss) private var dismiss
    let asset: MarketAsset?
    @ObservedObject var model: TradingObservableModel
    @State private var quantityText = "1"
    @State private var resultMessage: String?
    @State private var showReview = false
    @State private var reviewBuy = true
    @State private var reviewQuantity: Int32 = 1

    var body: some View {
        NavigationStack {
            Group {
                if let asset {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 18) {
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(asset.symbol)
                                        .font(.title.bold())
                                        .foregroundStyle(TradingPalette.ink)
                                    Text(asset.companyName)
                                        .foregroundStyle(TradingPalette.muted)
                                }
                                Spacer()
                                Text(formatMoney(asset.price))
                                    .font(.title3.bold())
                                    .foregroundStyle(TradingPalette.ink)
                            }

                            HStack {
                                Text("\(asset.exchange) · \(asset.category)")
                                Spacer()
                                Text(formatPercent(asset.changePercent))
                                    .foregroundStyle(asset.changePercent >= 0 ? TradingPalette.green : TradingPalette.red)
                            }
                            .font(.subheadline)
                            .foregroundStyle(TradingPalette.muted)

                            Button(model.state.watchlist.contains(asset.symbol) ? "Remove from watchlist" : "Add to watchlist") {
                                model.viewModel.toggleWatchlist(symbol: asset.symbol)
                            }
                            .disabled(model.state.isBusy)
                            Text("Illustrative trend · not historical market data").font(.caption)
                            Sparkline(
                                values: asset.history.map { $0.doubleValue },
                                positive: asset.changePercent >= 0
                            )
                                .frame(height: 150)
                                .padding(12)
                                .background(.white, in: RoundedRectangle(cornerRadius: 18))

                            Text("Cash available  \(formatMoney(model.state.cash))")
                                .font(.subheadline)
                                .foregroundStyle(TradingPalette.muted)

                            TextField("Number of whole shares", text: $quantityText)
                                .keyboardType(.numberPad)
                                .padding(14)
                                .background(.white, in: RoundedRectangle(cornerRadius: 14))

                            HStack(spacing: 12) {
                                orderButton("Simulate buy", isBuy: true, asset: asset)
                                orderButton("Simulate sell", isBuy: false, asset: asset)
                            }
                            DemoDisclaimer()
                        }
                        .padding(20)
                    }
                    .background(TradingPalette.canvas.ignoresSafeArea())
                } else {
                    EmptyMessage(text: "This sample asset is no longer available.")
                        .padding()
                }
            }
            .navigationTitle("Asset details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
            .confirmationDialog("Review paper order", isPresented: $showReview, titleVisibility: .visible) {
                Button("Confirm paper order") {
                    guard let asset else { return }
                    let result = model.viewModel.placeOrder(symbol: asset.symbol, isBuy: reviewBuy, quantity: reviewQuantity)
                    resultMessage = result.message
                    showReview = false
                }
            } message: {
                Text("\(reviewBuy ? "BUY" : "SELL") \(reviewQuantity) \(asset?.symbol ?? "") · estimated \(formatMoney((asset?.price ?? 0) * Double(reviewQuantity))) · no real money")
            }
            .alert("Demo order", isPresented: Binding(
                get: { resultMessage != nil },
                set: { if !$0 { resultMessage = nil } }
            )) {
                Button("OK", role: .cancel) { resultMessage = nil }
            } message: {
                Text(resultMessage ?? "")
            }
        }
    }

    private func orderButton(_ title: String, isBuy: Bool, asset: MarketAsset) -> some View {
        Button(title) {
            guard let quantity = Int32(quantityText), quantity >= 1, quantity <= 1_000_000 else {
                resultMessage = "Enter a whole-share quantity of at least 1."
                return
            }
            reviewQuantity = quantity
            reviewBuy = isBuy
            showReview = true
        }
        .disabled(model.state.isBusy || model.state.canRetryOrder)
        .font(.subheadline.bold())
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .foregroundStyle(.white)
        .background(isBuy ? TradingPalette.green : TradingPalette.ink, in: RoundedRectangle(cornerRadius: 14))
    }
}
