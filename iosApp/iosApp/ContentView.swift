/** Composes native feature screens and account navigation. */
import SwiftUI
import SharedLogic
import Foundation

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
