/** Owns WatchlistScreen presentation responsibilities; shared domain rules remain in Kotlin. */
import SwiftUI
import SharedLogic
import Foundation

struct WatchlistScreen: View {
    @ObservedObject var model: TradingObservableModel
    let onSelect: (String) -> Void

    var body: some View {
        List {
            if model.state.watchlist.isEmpty {
                Text("Open a stock in Discover and add it to your watchlist.")
            }
            ForEach(model.state.assets.filter { model.state.watchlist.contains($0.symbol) }, id: \.symbol) { asset in
                Button { onSelect(asset.symbol) } label: {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(asset.symbol).font(.headline)
                            Text(asset.companyName).font(.caption)
                        }
                        Spacer()
                        Text(formatMoney(asset.price))
                    }
                }
            }
        }
        .navigationTitle("Watchlist")
    }
}
