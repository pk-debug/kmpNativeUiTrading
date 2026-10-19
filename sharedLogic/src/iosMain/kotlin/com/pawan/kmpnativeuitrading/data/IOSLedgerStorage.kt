package com.pawan.kmpnativeuitrading.data

import com.pawan.kmpnativeuitrading.domain.repository.LedgerStorage
import com.pawan.kmpnativeuitrading.presentation.TradingViewModel
import platform.Foundation.NSUserDefaults

private class IOSLedgerStorage : LedgerStorage {
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
    TradingViewModel(DefaultTradingRepository(IOSLedgerStorage()))
