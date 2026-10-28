/**
 * Renders offline entry and authenticated account entry. Shared auth state controls progress, errors and navigation.
 */
package com.pawan.kmpnativeuitrading.features.auth.ui

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
import com.pawan.kmpnativeuitrading.features.trading.ui.TradingApp
import java.text.NumberFormat
import java.util.Locale

private val Ink = Color(0xFF152B2A)
private val Muted = Color(0xFF71817D)
private val Green = Color(0xFF138A63)
private val Red = Color(0xFFD45656)
private val BackgroundColor = Color(0xFFF5F7F5)
@Composable
fun NorthstarApp(authViewModel: AuthViewModel, tradingViewModel: TradingViewModel) {
    val authState = remember(authViewModel) { mutableStateOf(authViewModel.state) }
    DisposableEffect(authViewModel) {
        val observer = object : AuthStateObserver {
            override fun onAuthStateChanged(state: AuthState) {
                authState.value = state
            }
        }
        val observerId = authViewModel.addObserver(observer)
        onDispose { authViewModel.removeObserver(observerId) }
    }
    if (authState.value.isLoggedIn) {
        TradingApp(tradingViewModel, authViewModel::signOut, authViewModel::deleteAccount, authState.value)
    } else {
        LoginScreen(authViewModel, authState.value)
    }
}

@Composable
private fun LoginScreen(authViewModel: AuthViewModel, authState: AuthState) {
    var register by rememberSaveable { mutableStateOf(false) }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf(authState.errorMessage) }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Green,
            background = BackgroundColor,
            surface = Color.White,
            onBackground = Ink,
            onSurface = Ink,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("NORTHSTAR", color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                Text("Welcome back", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(if (authState.isRemote) "Your private paper-trading account" else "Offline practice · no real money", color = Muted)
                Spacer(Modifier.height(4.dp))
                if (authState.isRemote) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (authState.isRemote) "Username" else "Demo phone number") },
                    placeholder = { Text(if (authState.isRemote) "trader.pawan" else "+91 98765 43210") },
                    keyboardOptions = KeyboardOptions(keyboardType = if (authState.isRemote) KeyboardType.Text else KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                }
                (authState.errorMessage ?: errorMessage)?.let {
                    Text(it, color = Red, fontSize = 13.sp)
                }
                Button(
                    onClick = {
                        val (success, message) = if (!authState.isRemote) authViewModel.login("0000000000", "offline-demo-only") else if (register) authViewModel.register(phoneNumber, password) else authViewModel.login(phoneNumber, password)
                        errorMessage = if (success) null else message
                    },
                    enabled = !authState.isBusy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(if (authState.isBusy) "Connecting…" else if (register) "Create paper account" else if (!authState.isRemote) "Enter offline demo" else "Sign in", fontWeight = FontWeight.SemiBold)
                }
                if (authState.isRemote) TextButton(onClick = { register = !register; errorMessage = null }, enabled = !authState.isBusy) {
                    Text(if (register) "Already have an account? Sign in" else "New here? Create an account")
                }
                HorizontalDivider(color = Color(0xFFE4E9E6))
                Text(
                    if (authState.isRemote) "Use a unique username and a password of at least 12 characters. Your session stays in memory. This is paper trading, not a brokerage account." else "No account or password needed. Practice with sample money on this device. Prices are illustrative.",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
