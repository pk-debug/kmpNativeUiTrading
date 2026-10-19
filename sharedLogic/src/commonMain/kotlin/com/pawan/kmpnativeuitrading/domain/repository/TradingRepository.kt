package com.pawan.kmpnativeuitrading.domain.repository

import com.pawan.kmpnativeuitrading.domain.model.TradingLedger

interface TradingRepository {
    fun loadLedger(): TradingLedger
    fun saveLedger(ledger: TradingLedger)
}

interface LedgerStorage {
    fun readLedger(): String?
    fun writeLedger(value: String)
}
