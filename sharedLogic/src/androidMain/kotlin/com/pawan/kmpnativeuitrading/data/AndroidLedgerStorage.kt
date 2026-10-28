package com.pawan.kmpnativeuitrading.data

import android.content.Context
import com.pawan.kmpnativeuitrading.domain.repository.LedgerStorage
import com.pawan.kmpnativeuitrading.presentation.TradingViewModel

internal class AndroidLedgerStorage(context: Context) : LedgerStorage {
    private val preferences = context.applicationContext
        .getSharedPreferences("northstar_demo_account", Context.MODE_PRIVATE)

    override fun readLedger(): String? = preferences.getString(LEDGER_KEY, null)

    override fun writeLedger(value: String) {
        check(preferences.edit().putString(LEDGER_KEY, value).commit()) {
            "The demo account could not be saved on this device."
        }
    }

    private companion object {
        const val LEDGER_KEY = "ledger_v1"
    }
}

fun createTradingViewModel(context: Context): TradingViewModel =
    TradingViewModel(DefaultTradingRepository(AndroidLedgerStorage(context)))
