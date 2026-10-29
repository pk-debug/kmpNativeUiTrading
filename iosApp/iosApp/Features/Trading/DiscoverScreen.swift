/** Owns DiscoverScreen presentation responsibilities; shared domain rules remain in Kotlin. */
import SwiftUI
import SharedLogic
import Foundation

struct DiscoverScreen: View {
    @ObservedObject var model: TradingObservableModel
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("NORTHSTAR")
                        .font(.caption.weight(.bold))
                        .tracking(1.5)
                        .foregroundStyle(TradingPalette.green)
                    Text("Discover")
                        .font(.largeTitle.bold())
                        .foregroundStyle(TradingPalette.ink)
                    Text(model.state.isRemote ? "API paper account · synthetic INR prices" : "Indian markets · Offline paper account")
                        .font(.subheadline)
                        .foregroundStyle(TradingPalette.muted)
                    if model.state.isRemote {
                        Text(model.state.isBusy ? "Syncing…" : "Snapshot data · open Account to refresh or retry")
                            .font(.caption)
                        if model.state.canRetryOrder {
                            Text("An order is unconfirmed. Open Account to retry safely.").foregroundStyle(.red)
                        }
                        if let message = model.state.lastMessage { Text(message).font(.caption) }
                    }
                }

                AccountSummaryCard(
                    title: "Total account value",
                    value: formatMoney(model.state.cash + model.state.portfolio.marketValue),
                    subtitle: "Cash \(formatMoney(model.state.cash))"
                )

                HStack(spacing: 10) {
                    ForEach(model.state.indexes, id: \.name) { index in
                        MarketIndexCard(index: index)
                    }
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("Stocks")
                        .font(.title3.bold())
                        .foregroundStyle(TradingPalette.ink)
                    HStack(spacing: 10) {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(TradingPalette.muted)
                        TextField(
                            "Search company or ticker",
                            text: Binding(
                                get: { model.state.searchText },
                                set: { model.viewModel.setSearchText(value: $0) }
                            )
                        )
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.characters)
                    }
                    .padding(13)
                    .background(.white, in: RoundedRectangle(cornerRadius: 14))

                    Picker("Sort sample stocks", selection: Binding(get: { model.state.marketSort }, set: { model.viewModel.setMarketSort(value: $0) })) {
                        Text("A–Z").tag("NAME")
                        Text("Top gainers").tag("GAINERS")
                        Text("Top losers").tag("LOSERS")
                    }.pickerStyle(.segmented)
                    Picker("Sector", selection: Binding(get: { model.state.selectedCategory }, set: { model.viewModel.setMarketCategory(value: $0) })) {
                        Text("All sectors").tag("")
                        ForEach(model.state.marketCategories, id: \.self) { sector in Text(sector).tag(sector) }
                    }
                    Text("Rankings use illustrative changes, not live performance.").font(.caption)
                    if model.state.filteredAssets.isEmpty {
                        EmptyMessage(text: "No stocks match. Clear your search or select All sectors.")
                    } else {
                        ForEach(model.state.filteredAssets, id: \.symbol) { asset in
                            AssetRow(asset: asset)
                                .contentShape(Rectangle())
                                .onTapGesture { onSelect(asset.symbol) }
                        }
                    }
                }
                LearningBasics()
                DemoDisclaimer()
            }
            .padding(20)
        }
        .background(TradingPalette.canvas.ignoresSafeArea())
        .navigationBarHidden(true)
    }
}
