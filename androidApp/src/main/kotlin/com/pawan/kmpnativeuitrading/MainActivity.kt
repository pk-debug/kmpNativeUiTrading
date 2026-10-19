package com.pawan.kmpnativeuitrading

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pawan.kmpnativeuitrading.data.createTradingViewModel

class MainActivity : ComponentActivity() {
    private val tradingViewModel by lazy { createTradingViewModel(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TradingApp(tradingViewModel)
        }
    }
}
