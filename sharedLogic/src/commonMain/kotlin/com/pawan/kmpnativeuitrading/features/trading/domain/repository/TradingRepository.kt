/**
 * Defines TradingRepository for the shared native application. Keep platform UI concerns outside domain rules.
 */
package com.pawan.kmpnativeuitrading.features.trading.domain.repository

import com.pawan.kmpnativeuitrading.features.trading.domain.model.TradingLedger

interface TradingRepository {
    fun loadLedger(): TradingLedger
    fun saveLedger(ledger: TradingLedger)
}

interface LedgerStorage {
    fun readLedger(): String?
    fun writeLedger(value: String)
}
