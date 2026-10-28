/**
 * Native SwiftUI presentation and weak observer bridges for the shared feature models. Lazy collections avoid eagerly constructing every row.
 */
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

    init(viewModel: TradingViewModel) {
        self.viewModel = viewModel
        state = viewModel.state
        super.init()
        observerId = viewModel.addObserver(observer: TradingObserverProxy(owner: self))
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

private final class AuthObservableModel: NSObject, ObservableObject, AuthStateObserver {
    @Published private(set) var state: AuthState
    let viewModel: AuthViewModel
    private var observerId: Int64 = 0

    init(viewModel: AuthViewModel) {
        self.viewModel = viewModel
        state = viewModel.state
        super.init()
        observerId = viewModel.addObserver(observer: AuthObserverProxy(owner: self))
    }

    func onAuthStateChanged(state: AuthState) {
        DispatchQueue.main.async {
            self.state = state
        }
    }

    deinit {
        viewModel.removeObserver(id: observerId)
    }
}

// Kotlin owns its observer. A weak proxy avoids a model -> VM -> model retain cycle.
private final class TradingObserverProxy: NSObject, TradingStateObserver {
    weak var owner: TradingObservableModel?
    init(owner: TradingObservableModel) { self.owner = owner; super.init() }
    func onStateChanged(state: TradingState) { owner?.onStateChanged(state: state) }
}

private final class AuthObserverProxy: NSObject, AuthStateObserver {
    weak var owner: AuthObservableModel?
    init(owner: AuthObservableModel) { self.owner = owner; super.init() }
    func onAuthStateChanged(state: AuthState) { owner?.onAuthStateChanged(state: state) }
}

struct ContentView: View {
    @StateObject private var model: TradingObservableModel
    @StateObject private var authModel: AuthObservableModel

    init() {
        let session = AppSessionFactoryKt.createAppSession()
        _model = StateObject(wrappedValue: TradingObservableModel(viewModel: session.trading))
        _authModel = StateObject(wrappedValue: AuthObservableModel(viewModel: session.auth))
    }
    @State private var selectedTab = 0
    @State private var selectedSymbol: String?
    @State private var showAccount = false
    @State private var confirmDelete = false

    var body: some View {
        Group {
            if authModel.state.isLoggedIn {
                tradingTabs
            } else {
                LoginScreen(model: authModel)
            }
        }
        .tint(TradingPalette.green)
        .onChange(of: authModel.state.isLoggedIn) { _, _ in
            showAccount = false
            selectedSymbol = nil
            selectedTab = 0
        }
    }

    private var tradingTabs: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                DiscoverScreen(model: model, onSelect: { selectedSymbol = $0 })
            }
            .toolbar { signOutButton }
            .tabItem { Label("Discover", systemImage: "magnifyingglass") }
            .tag(0)

            NavigationStack {
                WatchlistScreen(model: model, onSelect: { selectedSymbol = $0 })
            }
            .toolbar { signOutButton }
            .tabItem { Label("Watchlist", systemImage: "star") }
            .tag(3)

            NavigationStack {
                PortfolioScreen(model: model, onSelect: { selectedSymbol = $0 })
            }
            .toolbar { signOutButton }
            .tabItem { Label("Portfolio", systemImage: "chart.pie") }
            .tag(1)

            NavigationStack {
                ActivityScreen(state: model.state)
            }
            .toolbar { signOutButton }
            .tabItem { Label("Activity", systemImage: "clock.arrow.circlepath") }
            .tag(2)
        }
        .sheet(isPresented: $showAccount) {
            NavigationStack {
                Form {
                    Section("Paper trading") {
                        Text(model.state.isRemote ? "API mode · INR · synthetic quotes" : "Offline practice · INR · synthetic quotes")
                        Text("No deposits, withdrawals or real exchange orders.")
                    }
                    Section("Privacy") {
                        Text("No analytics or advertising SDK. API mode sends credentials over HTTPS and stores practice activity on your server. Session tokens stay only in memory and expire after 8 hours.")
                    }
                    if model.state.isRemote {
                        Section("Sync") {
                            Button("Refresh account") { model.viewModel.refresh() }
                                .disabled(model.state.isBusy)
                            if model.state.canRetryOrder {
                                Button("Retry unconfirmed order safely") { model.viewModel.retryOrder() }
                                    .disabled(model.state.isBusy)
                            }
                            Text(model.state.lastMessage ?? "Quotes are illustrative, not live.")
                        }
                        Section {
                            Button("Delete account and server data", role: .destructive) { confirmDelete = true }
                                .disabled(authModel.state.isBusy)
                        }
                    }
                    if let error = authModel.state.errorMessage { Text(error).foregroundStyle(.red) }
                    Button("Sign out") { authModel.viewModel.signOut() }
                        .disabled(authModel.state.isBusy)
                }
                .navigationTitle("Account")
                .toolbar { Button("Close") { showAccount = false } }
                .confirmationDialog("Permanently delete all account data?", isPresented: $confirmDelete, titleVisibility: .visible) {
                    Button("Delete permanently", role: .destructive) { authModel.viewModel.deleteAccount() }
                } message: { Text("Your profile, sessions, holdings, orders and watchlist will be removed. This cannot be undone.") }
            }
        }
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

    @ToolbarContentBuilder
    private var signOutButton: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Button("Account") { showAccount = true }
        }
    }
}

private struct LoginScreen: View {
    @ObservedObject var model: AuthObservableModel
    @State private var register = false
    @State private var phoneNumber = ""
    @State private var password = ""
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                Text("NORTHSTAR")
                    .font(.caption.weight(.bold))
                    .tracking(1.6)
                    .foregroundStyle(TradingPalette.green)
                Text("Welcome back")
                    .font(.largeTitle.bold())
                    .foregroundStyle(TradingPalette.ink)
                Text(model.state.isRemote ? "Your private paper-trading account" : "Offline practice · no real money")
                    .font(.subheadline)
                    .foregroundStyle(TradingPalette.muted)

                if model.state.isRemote {
                TextField(model.state.isRemote ? "Username" : "Demo phone number", text: $phoneNumber)
                    .keyboardType(model.state.isRemote ? .asciiCapable : .phonePad)
                    .textContentType(.telephoneNumber)
                    .textInputAutocapitalization(.never)
                    .padding(14)
                    .background(.white, in: RoundedRectangle(cornerRadius: 14))

                SecureField("Password", text: $password)
                    .textContentType(.password)
                    .padding(14)
                    .background(.white, in: RoundedRectangle(cornerRadius: 14))

                }
                if let message = model.state.errorMessage ?? errorMessage {
                    Text(message)
                        .font(.caption)
                        .foregroundStyle(TradingPalette.red)
                }

                Button {
                    let result = !model.state.isRemote
                        ? model.viewModel.login(phoneNumber: "0000000000", password: "offline-demo-only")
                        : register
                        ? model.viewModel.register(username: phoneNumber, password: password)
                        : model.viewModel.login(phoneNumber: phoneNumber, password: password)
                    errorMessage = result.success ? nil : result.message
                } label: {
                    Text(model.state.isBusy ? "Connecting…" : (register ? "Create paper account" : (model.state.isRemote ? "Sign in" : "Enter offline demo")))
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 15)
                        .background(TradingPalette.green, in: RoundedRectangle(cornerRadius: 14))
                }

                .disabled(model.state.isBusy)

                if model.state.isRemote {
                    Button(register ? "Already have an account? Sign in" : "Create an account") { register.toggle(); errorMessage = nil }
                        .disabled(model.state.isBusy)
                    Text("Use a unique username and a password of at least 12 characters. This is a practice account, not a brokerage account.")
                        .font(.caption)
                }
                Text(model.state.isRemote ? "Passwords are verified by your server; they are never stored on this phone." : "No account or password needed. Practice with sample money on this device. Prices are illustrative.")
                    .font(.caption)
                    .foregroundStyle(TradingPalette.muted)
            }
            .padding(24)
            .frame(maxWidth: 480)
        }
        .background(TradingPalette.canvas.ignoresSafeArea())
    }
}

private struct DiscoverScreen: View {
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

/// Currency formatting is reused on the main UI thread instead of allocating per row.
private let moneyFormatter: NumberFormatter = {
    let formatter = NumberFormatter()
    formatter.numberStyle = .currency
    formatter.locale = Locale(identifier: "en_IN")
    formatter.currencyCode = "INR"
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter
}()

private func formatMoney(_ amount: Double) -> String {
    return moneyFormatter.string(from: NSNumber(value: amount)) ?? "₹\(amount)"
}

private func formatPercent(_ change: Double) -> String {
    let sign = change >= 0 ? "+" : ""
    return "\(sign)\(String(format: "%.2f", change))%"
}


/// Watchlists reuse the shared state, so Android and iOS follow the same rules.
private struct WatchlistScreen: View {
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
