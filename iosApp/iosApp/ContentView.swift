import SwiftUI
import SharedLogic
import Foundation

private enum TradingPalette {
    static let ink = Color(red: 0.08, green: 0.17, blue: 0.16)
    static let muted = Color(red: 0.43, green: 0.51, blue: 0.49)
    static let green = Color(red: 0.08, green: 0.54, blue: 0.39)
    static let red = Color(red: 0.82, green: 0.30, blue: 0.30)
    static let canvas = Color(red: 0.96, green: 0.97, blue: 0.96)
}

private final class TradingObservableModel: NSObject, ObservableObject, TradingStateObserver {
    @Published private(set) var state: TradingState
    let viewModel: TradingViewModel
    private var observerId: Int64 = 0

    override init() {
        viewModel = IOSLedgerStorageKt.createTradingViewModel()
        state = viewModel.state
        super.init()
        observerId = viewModel.addObserver(observer: self)
    }

    func onStateChanged(state: TradingState) {
        DispatchQueue.main.async {
            self.state = state
        }
    }

    deinit {
        viewModel.removeObserver(id: observerId)
    }
}

struct ContentView: View {
    @StateObject private var model = TradingObservableModel()
    @State private var selectedTab = 0
    @State private var selectedSymbol: String?

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                DiscoverScreen(model: model, onSelect: { selectedSymbol = $0 })
            }
            .tabItem { Label("Discover", systemImage: "magnifyingglass") }
            .tag(0)

            NavigationStack {
                PortfolioScreen(model: model, onSelect: { selectedSymbol = $0 })
            }
            .tabItem { Label("Portfolio", systemImage: "chart.pie") }
            .tag(1)

            NavigationStack {
                ActivityScreen(state: model.state)
            }
            .tabItem { Label("Activity", systemImage: "clock.arrow.circlepath") }
            .tag(2)
        }
        .tint(TradingPalette.green)
        .sheet(isPresented: Binding(
            get: { selectedSymbol != nil },
            set: { if !$0 { selectedSymbol = nil } }
        )) {
            if let symbol = selectedSymbol {
                AssetDetailScreen(
                    asset: model.state.assets.first(where: { $0.symbol == symbol }),
                    model: model
                )
            }
        }
    }
}

private struct DiscoverScreen: View {
    @ObservedObject var model: TradingObservableModel
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("NORTHSTAR")
                        .font(.caption.weight(.bold))
                        .tracking(1.5)
                        .foregroundStyle(TradingPalette.green)
                    Text("Discover")
                        .font(.largeTitle.bold())
                        .foregroundStyle(TradingPalette.ink)
                    Text("Indian markets · Demo account")
                        .font(.subheadline)
                        .foregroundStyle(TradingPalette.muted)
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

                    if model.state.filteredAssets.isEmpty {
                        EmptyMessage(text: "No sample stocks match your search.")
                    } else {
                        ForEach(model.state.filteredAssets, id: \.symbol) { asset in
                            AssetRow(asset: asset)
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
        .navigationBarHidden(true)
    }
}

private struct PortfolioScreen: View {
    @ObservedObject var model: TradingObservableModel
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Portfolio")
                        .font(.largeTitle.bold())
                        .foregroundStyle(TradingPalette.ink)
                    Text("Your locally saved demo holdings")
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

private struct ActivityScreen: View {
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

private struct AssetDetailScreen: View {
    @Environment(\.dismiss) private var dismiss
    let asset: MarketAsset?
    @ObservedObject var model: TradingObservableModel
    @State private var quantityText = "1"
    @State private var resultMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if let asset {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
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
            guard let quantity = Int32(quantityText) else {
                resultMessage = "Enter a whole-share quantity of at least 1."
                return
            }
            let result = model.viewModel.placeOrder(
                symbol: asset.symbol,
                isBuy: isBuy,
                quantity: quantity
            )
            resultMessage = result.message
        }
        .font(.subheadline.bold())
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .foregroundStyle(.white)
        .background(isBuy ? TradingPalette.green : TradingPalette.ink, in: RoundedRectangle(cornerRadius: 14))
    }
}

private struct AccountSummaryCard: View {
    let title: String
    let value: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title).font(.subheadline).foregroundStyle(.white.opacity(0.75))
            Text(value).font(.system(size: 30, weight: .bold)).foregroundStyle(.white)
            Text(subtitle).font(.caption).foregroundStyle(.white.opacity(0.75))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(22)
        .background(TradingPalette.ink, in: RoundedRectangle(cornerRadius: 22))
    }
}

private struct MarketIndexCard: View {
    let index: MarketIndex

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(index.name).font(.caption2.weight(.semibold)).foregroundStyle(TradingPalette.muted)
            Text(index.value).font(.subheadline.bold()).foregroundStyle(TradingPalette.ink)
            Text(index.change)
                .font(.caption)
                .foregroundStyle(index.isPositive ? TradingPalette.green : TradingPalette.red)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.white, in: RoundedRectangle(cornerRadius: 16))
    }
}

private struct AssetRow: View {
    let asset: MarketAsset

    var body: some View {
        HStack(spacing: 12) {
            SymbolBadge(symbol: asset.symbol)
            VStack(alignment: .leading, spacing: 3) {
                Text(asset.symbol).font(.subheadline.bold()).foregroundStyle(TradingPalette.ink)
                Text(asset.companyName).font(.caption).foregroundStyle(TradingPalette.muted).lineLimit(1)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 3) {
                Text(formatMoney(asset.price)).font(.subheadline.weight(.semibold)).foregroundStyle(TradingPalette.ink)
                Text(formatPercent(asset.changePercent))
                    .font(.caption)
                    .foregroundStyle(asset.changePercent >= 0 ? TradingPalette.green : TradingPalette.red)
            }
        }
        .padding(15)
        .background(.white, in: RoundedRectangle(cornerRadius: 18))
    }
}

private struct PositionRow: View {
    let position: Position
    let asset: MarketAsset
    private var gainLoss: Double { (asset.price - position.averageCost) * Double(position.quantity) }

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(position.symbol).font(.subheadline.bold()).foregroundStyle(TradingPalette.ink)
                Text("\(position.quantity) shares · Avg \(formatMoney(position.averageCost))")
                    .font(.caption)
                    .foregroundStyle(TradingPalette.muted)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text(formatMoney(asset.price * Double(position.quantity)))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(TradingPalette.ink)
                Text(formatMoney(gainLoss))
                    .font(.caption)
                    .foregroundStyle(gainLoss >= 0 ? TradingPalette.green : TradingPalette.red)
            }
        }
        .padding(15)
        .background(.white, in: RoundedRectangle(cornerRadius: 18))
    }
}

private struct OrderRow: View {
    let order: SimulatedOrder

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(order.side.name) \(order.symbol)")
                    .font(.subheadline.bold())
                    .foregroundStyle(TradingPalette.ink)
                Text("\(order.quantity) shares · \(formatMoney(order.price)) each")
                    .font(.caption)
                    .foregroundStyle(TradingPalette.muted)
            }
            Spacer()
            Text(formatMoney(order.total))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(TradingPalette.ink)
        }
        .padding(15)
        .background(.white, in: RoundedRectangle(cornerRadius: 18))
    }
}

private struct SummaryValue: View {
    let title: String
    let value: String
    var positive: Bool?

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title).font(.caption).foregroundStyle(TradingPalette.muted)
            Text(value)
                .font(.subheadline.bold())
                .foregroundStyle(positive.map { $0 ? TradingPalette.green : TradingPalette.red } ?? TradingPalette.ink)
        }
    }
}

private struct SymbolBadge: View {
    let symbol: String

    var body: some View {
        Text(String(symbol.prefix(1)))
            .font(.headline.bold())
            .foregroundStyle(TradingPalette.green)
            .frame(width: 42, height: 42)
            .background(Color(red: 0.91, green: 0.96, blue: 0.93), in: Circle())
    }
}

private struct Sparkline: View {
    let values: [Double]
    let positive: Bool

    var body: some View {
        GeometryReader { geometry in
            Path { path in
                guard values.count > 1, let minimum = values.min(), let maximum = values.max() else { return }
                let range = max(maximum - minimum, 1)
                for (index, value) in values.enumerated() {
                    let point = CGPoint(
                        x: geometry.size.width * CGFloat(index) / CGFloat(values.count - 1),
                        y: geometry.size.height * (1 - CGFloat((value - minimum) / range))
                    )
                    if index == 0 { path.move(to: point) } else { path.addLine(to: point) }
                }
            }
            .stroke(positive ? TradingPalette.green : TradingPalette.red, style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
        }
    }
}

private struct EmptyMessage: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(TradingPalette.muted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(18)
            .background(.white, in: RoundedRectangle(cornerRadius: 18))
    }
}

private struct DemoDisclaimer: View {
    var body: some View {
        Text("Demo only. Prices are illustrative; no real orders or money are involved.")
            .font(.caption2)
            .foregroundStyle(TradingPalette.muted)
            .padding(.vertical, 8)
    }
}

private func formatMoney(_ amount: Double) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .currency
    formatter.locale = Locale(identifier: "en_IN")
    formatter.currencyCode = "INR"
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter.string(from: NSNumber(value: amount)) ?? "₹\(amount)"
}

private func formatPercent(_ change: Double) -> String {
    let sign = change >= 0 ? "+" : ""
    return "\(sign)\(String(format: "%.2f", change))%"
}
