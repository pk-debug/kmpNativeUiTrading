/**
 * Implements AndroidLedgerStorage using native device persistence. No broker secret or password belongs in this storage.
 */
package com.pawan.kmpnativeuitrading.core.platform

import com.pawan.kmpnativeuitrading.features.trading.data.local.DefaultTradingRepository
import android.content.Context
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.LedgerStorage
import com.pawan.kmpnativeuitrading.features.trading.presentation.TradingViewModel

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
