/**
 * Renders the trading feature with lazy Android lists and explicit order review. Business decisions stay in shared Kotlin.
 */
package com.pawan.kmpnativeuitrading.features.trading.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pawan.kmpnativeuitrading.features.trading.domain.model.MarketAsset
import com.pawan.kmpnativeuitrading.features.auth.domain.model.AuthState
import com.pawan.kmpnativeuitrading.features.trading.domain.model.MarketIndex
import com.pawan.kmpnativeuitrading.features.trading.domain.model.Position
import com.pawan.kmpnativeuitrading.features.trading.domain.model.SimulatedOrder
import com.pawan.kmpnativeuitrading.features.trading.domain.model.TradingState
import com.pawan.kmpnativeuitrading.features.trading.presentation.TradingStateObserver
import com.pawan.kmpnativeuitrading.features.trading.presentation.TradingViewModel
import com.pawan.kmpnativeuitrading.features.auth.presentation.AuthStateObserver
import com.pawan.kmpnativeuitrading.features.auth.presentation.AuthViewModel
import java.text.NumberFormat
import java.util.Locale

private val Ink = Color(0xFF152B2A)
private val Muted = Color(0xFF71817D)
private val Green = Color(0xFF138A63)
private val Red = Color(0xFFD45656)
private val BackgroundColor = Color(0xFFF5F7F5)
private val Currency = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
    maximumFractionDigits = 2
}

@Composable
fun TradingApp(viewModel: TradingViewModel, onSignOut: () -> Unit, onDeleteAccount: () -> Unit = {}, authState: AuthState = AuthState(null, null)) {
    var showAccount by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val stateHolder = remember(viewModel) { mutableStateOf(viewModel.state) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedAsset by remember { mutableStateOf<MarketAsset?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(viewModel) {
        val observer = object : TradingStateObserver {
            override fun onStateChanged(state: TradingState) {
                stateHolder.value = state
            }
        }
        val observerId = viewModel.addObserver(observer)
        onDispose { viewModel.removeObserver(observerId) }
    }
    LaunchedEffect(stateHolder.value.lastMessage) {
        stateHolder.value.lastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Green,
            background = BackgroundColor,
            surface = Color.White,
            onBackground = Ink,
            onSurface = Ink,
        ),
    ) {
        Scaffold(
            containerColor = BackgroundColor,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Row(
                    Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("NORTHSTAR", color = Green, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showAccount = true }) { Text("Account") }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf("Discover", "Watchlist", "Portfolio", "Activity").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Text(listOf("⌕", "☆", "◉", "↔")[index], fontSize = 22.sp) },
                            label = { Text(label) },
                        )
                    }
                }
            },
        ) { padding ->
            when (selectedTab) {
                0 -> DiscoverScreen(
                    state = stateHolder.value,
                    onSearch = viewModel::setSearchText,
                    onAssetSelected = { selectedAsset = it },
                    modifier = Modifier.padding(padding),
                )
                1 -> DiscoverScreen(
                    state = stateHolder.value.copy(filteredAssets = stateHolder.value.filteredAssets.filter { it.symbol in stateHolder.value.watchlist }),
                    onSearch = viewModel::setSearchText,
                    onAssetSelected = { selectedAsset = it },
                    modifier = Modifier.padding(padding),
                    watchlistOnly = true,
                )
                2 -> PortfolioScreen(
                    state = stateHolder.value,
                    onAssetSelected = { selectedAsset = it },
                    modifier = Modifier.padding(padding),
                )
                else -> ActivityScreen(stateHolder.value.orders, Modifier.padding(padding))
            }
        }
    }

    if (showAccount) AlertDialog(
        onDismissRequest = { showAccount = false },
        title = { Text("Your paper account") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (stateHolder.value.isRemote) "API mode · INR · synthetic prices" else "Offline mode · INR · synthetic prices")
            Text("No deposits, withdrawals or real exchange orders. Watchlists and holdings are practice data.")
            Text("Privacy: no advertising or analytics SDK. API mode sends credentials over HTTPS and stores account activity on your server. Sessions expire after 8 hours and are kept only in memory on the phone.")
            authState.errorMessage?.let { Text(it, color = Red) }
            if (stateHolder.value.isRemote) {
                TextButton(onClick = viewModel::refresh, enabled = !stateHolder.value.isBusy) { Text("Refresh account") }
                if (stateHolder.value.canRetryOrder) TextButton(onClick = viewModel::retryOrder, enabled = !stateHolder.value.isBusy) { Text("Retry unconfirmed order safely") }
                TextButton(onClick = { confirmDelete = true }, enabled = !authState.isBusy) { Text("Delete account and server data", color = Red) }
            }
        } },
        confirmButton = { TextButton(onClick = onSignOut, enabled = !authState.isBusy) { Text(if (authState.isBusy) "Please wait…" else "Sign out") } },
        dismissButton = { TextButton(onClick = { showAccount = false }) { Text("Close") } },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Permanently delete your account?") },
        text = { Text("This removes your server profile, sessions, holdings, orders and watchlist. It cannot be undone.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteAccount() }) { Text("Delete permanently", color = Red) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep account") } },
    )
    selectedAsset?.let { asset ->
        AssetDetailDialog(
            asset = asset,
            state = stateHolder.value,
            onWatch = { viewModel.toggleWatchlist(asset.symbol) },
            onDismiss = { selectedAsset = null },
        ) { isBuy, quantity -> viewModel.placeOrder(asset.symbol, isBuy, quantity) }
    }
}

@Composable
private fun DiscoverScreen(
    state: TradingState,
    onSearch: (String) -> Unit,
    onAssetSelected: (MarketAsset) -> Unit,
    modifier: Modifier = Modifier,
    watchlistOnly: Boolean = false,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("NORTHSTAR", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(if (watchlistOnly) "Watchlist" else "Discover", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(if (state.isRemote) "API paper account · synthetic INR prices" else "Indian markets · Offline paper account", color = Muted, fontSize = 14.sp)
                if (state.isRemote) Text(if (state.isBusy) "Syncing…" else if (state.lastSyncedMillis == null) "Not synced · open Account to refresh" else "Snapshot synced · quotes are illustrative", color = Muted, fontSize = 12.sp)
                if (state.canRetryOrder) Text("An order is unconfirmed. Open Account to retry safely.", color = Red)
            }
        }
        item {
            AccountSummary(
                title = "Total account value",
                value = money(state.cash + state.portfolio.marketValue),
                subtitle = "Cash ${money(state.cash)}",
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.indexes.forEach { MarketIndexCard(it, Modifier.weight(1f)) }
            }
        }
        item {
            Text("Stocks", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.searchText,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search company or ticker") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        }
        if (state.filteredAssets.isEmpty()) {
            item { Text(if (watchlistOnly) "Your watchlist is empty. Open a stock in Discover and choose Add to watchlist." else "No sample stocks match your search.", color = Muted, modifier = Modifier.padding(12.dp)) }
        }
        items(state.filteredAssets, key = MarketAsset::symbol) { asset ->
            AssetRow(asset) { onAssetSelected(asset) }
        }
        item { DemoDisclaimer() }
    }
}

@Composable
private fun PortfolioScreen(
    state: TradingState,
    onAssetSelected: (MarketAsset) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Portfolio", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(if (state.isRemote) "Server-confirmed practice holdings" else "Your locally saved practice holdings", color = Muted)
        }
        item {
            AccountSummary(
                "Invested in stocks",
                money(state.portfolio.marketValue),
                "Cash ${money(state.cash)}",
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryValue("Total account", money(state.cash + state.portfolio.marketValue))
                    SummaryValue(
                        "Unrealized P/L",
                        money(state.portfolio.gainLoss),
                        state.portfolio.gainLoss >= 0,
                    )
                }
            }
        }
        item { Text("Holdings", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (state.positions.isEmpty()) {
            item { EmptyMessage("No holdings yet. Explore stocks to simulate a buy.") }
        }
        items(state.positions, key = Position::symbol) { position ->
            val asset = state.assets.firstOrNull { (symbol) -> symbol == position.symbol }
            asset?.let { PositionRow(position, it) { onAssetSelected(it) } }
        }
        item { DemoDisclaimer() }
    }
}

@Composable
private fun ActivityScreen(orders: List<SimulatedOrder>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Activity", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Your simulated orders, saved on this device", color = Muted)
        }
        if (orders.isEmpty()) {
            item { EmptyMessage("Your completed demo orders will appear here.") }
        }
        items(orders, key = SimulatedOrder::id) { order -> OrderRow(order) }
        item { DemoDisclaimer() }
    }
}

@Composable
private fun AccountSummary(title: String, value: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Ink),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
            Text(value, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun MarketIndexCard(index: MarketIndex, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(index.name, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(index.value, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(index.change, color = if (index.isPositive) Green else Red, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AssetRow(asset: MarketAsset, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolBadge(asset.symbol)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(asset.symbol, color = Ink, fontWeight = FontWeight.Bold)
                Text(asset.companyName, color = Muted, fontSize = 12.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(asset.price), color = Ink, fontWeight = FontWeight.SemiBold)
                Text(
                    "${if (asset.changePercent >= 0) "+" else ""}${asset.changePercent}%",
                    color = if (asset.changePercent >= 0) Green else Red,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun PositionRow(position: Position, asset: MarketAsset, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(position.symbol, color = Ink, fontWeight = FontWeight.Bold)
                Text("${position.quantity} shares · Avg ${money(position.averageCost)}", color = Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(asset.price * position.quantity), color = Ink, fontWeight = FontWeight.SemiBold)
                val gain = (asset.price - position.averageCost) * position.quantity
                Text(money(gain), color = if (gain >= 0) Green else Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun OrderRow(order: SimulatedOrder) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("${order.side.name} ${order.symbol}", color = Ink, fontWeight = FontWeight.Bold)
                Text("${order.quantity} shares · ${money(order.price)} each", color = Muted, fontSize = 12.sp)
            }
            Text(money(order.total), color = Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, positive: Boolean? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = positive?.let { if (it) Green else Red } ?: Ink, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SymbolBadge(symbol: String) {
    Surface(shape = CircleShape, color = Color(0xFFE9F4EF), modifier = Modifier.size(42.dp)) {
        BoxCentered { Text(symbol.take(1), color = Green, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun BoxCentered(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun EmptyMessage(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Text(message, color = Muted, modifier = Modifier.fillMaxWidth().padding(20.dp))
    }
}

@Composable
private fun DemoDisclaimer() {
    Text(
        "Demo only. Prices are illustrative; no real orders or money are involved.",
        color = Muted,
        fontSize = 11.sp,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun AssetDetailDialog(
    asset: MarketAsset,
    state: TradingState,
    onDismiss: () -> Unit,
    onWatch: () -> Unit,
    onOrder: (Boolean, Int) -> Unit,
) {
    var quantity by remember { mutableStateOf("1") }
    var validation by remember { mutableStateOf<String?>(null) }
    var reviewBuy by remember { mutableStateOf<Boolean?>(null) }
    var reviewQuantity by remember { mutableIntStateOf(1) }
    if (reviewBuy != null) {
        AlertDialog(
            onDismissRequest = { reviewBuy = null },
            title = { Text("Review paper order") },
            text = { Text("${if (reviewBuy == true) "BUY" else "SELL"} $reviewQuantity ${asset.symbol} shares\nEstimated value ${money(asset.price * reviewQuantity)}\nSynthetic market price · no real money") },
            confirmButton = { TextButton(onClick = { onOrder(reviewBuy == true, reviewQuantity); reviewBuy = null; onDismiss() }, enabled = !state.isBusy && !state.canRetryOrder) { Text("Confirm paper order") } },
            dismissButton = { TextButton(onClick = { reviewBuy = null }) { Text("Back") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(asset.symbol, color = Ink, fontWeight = FontWeight.Bold)
                Text(asset.companyName, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Normal)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(money(asset.price), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${if (asset.changePercent >= 0) "+" else ""}${asset.changePercent}%",
                        color = if (asset.changePercent >= 0) Green else Red,
                    )
                }
                Text("${asset.exchange} · ${asset.category}", color = Muted, fontSize = 12.sp)
                TextButton(onClick = onWatch, enabled = !state.isBusy) { Text(if (asset.symbol in state.watchlist) "Remove from watchlist" else "Add to watchlist") }
                Text("Illustrative trend · not historical market data", color = Muted, fontSize = 11.sp)
                PriceChart(asset.history, asset.changePercent >= 0)
                Text("Cash available ${money(state.cash)}", color = Muted, fontSize = 12.sp)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter(Char::isDigit) },
                    label = { Text("Whole shares") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                validation?.let { Text(it, color = Red, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val count = quantity.toIntOrNull()
                        if ((count == null) || (count !in 1..1_000_000)) validation = "Enter at least one whole share."
                        else {
                            reviewQuantity = count
                            reviewBuy = true
                        }
                    },
                    enabled = !state.isBusy && !state.canRetryOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                ) { Text("Review buy") }
                Button(
                    onClick = {
                        val count = quantity.toIntOrNull()
                        if ((count == null) || (count !in 1..1_000_000)) validation = "Enter at least one whole share."
                        else {
                            reviewQuantity = count
                            reviewBuy = false
                        }
                    },
                    enabled = !state.isBusy && !state.canRetryOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = Ink),
                ) { Text("Review sell") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun PriceChart(values: List<Double>, positive: Boolean) {
    val lineColor = if (positive) Green else Red
    Canvas(Modifier.fillMaxWidth().height(110.dp).background(Color(0xFFF3F7F4), RoundedCornerShape(12.dp)).padding(10.dp)) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: return@Canvas
        val range = (values.maxOrNull() ?: min) - min
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = (size.width * index) / values.lastIndex
            val y = size.height - if (range == 0.0) 0f else (((value - min) / range) * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun money(value: Double): String = Currency.format(value)
