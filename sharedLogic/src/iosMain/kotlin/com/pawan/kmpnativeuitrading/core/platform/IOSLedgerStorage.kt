/**
 * Implements IOSLedgerStorage using native device persistence. No broker secret or password belongs in this storage.
 */
package com.pawan.kmpnativeuitrading.core.platform

import com.pawan.kmpnativeuitrading.features.trading.data.local.DefaultTradingRepository
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.LedgerStorage
import com.pawan.kmpnativeuitrading.features.trading.presentation.TradingViewModel
import platform.Foundation.NSUserDefaults

internal class IOSLedgerStorage : LedgerStorage {
    override fun readLedger(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(LEDGER_KEY)

    override fun writeLedger(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = LEDGER_KEY)
    }

    private companion object {
        const val LEDGER_KEY = "northstar_demo_account_ledger_v1"
    }
}

fun createTradingViewModel(): TradingViewModel =
    TradingViewModel(com.pawan.kmpnativeuitrading.features.trading.data.local.LocalTradingSession(DefaultTradingRepository(IOSLedgerStorage())))
