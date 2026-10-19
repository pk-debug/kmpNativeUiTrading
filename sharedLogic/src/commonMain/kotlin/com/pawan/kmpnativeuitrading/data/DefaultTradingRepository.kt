package com.pawan.kmpnativeuitrading.data

import com.pawan.kmpnativeuitrading.domain.model.Position
import com.pawan.kmpnativeuitrading.domain.model.TradingLedger
import com.pawan.kmpnativeuitrading.domain.repository.LedgerStorage
import com.pawan.kmpnativeuitrading.domain.repository.TradingRepository
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class DefaultTradingRepository(
    private val storage: LedgerStorage,
) : TradingRepository {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun loadLedger(): TradingLedger {
        val stored = storage.readLedger() ?: return defaultLedger().also(::saveLedger)
        return try {
            json.decodeFromString<TradingLedger>(stored)
        } catch (exception: SerializationException) {
            throw IllegalStateException("The saved demo account could not be read.", exception)
        }
    }

    override fun saveLedger(ledger: TradingLedger) {
        storage.writeLedger(json.encodeToString(ledger))
    }

    private fun defaultLedger() = TradingLedger(
        cash = 1_000_000.0,
        positions = listOf(
            Position("RELIANCE", 8, 1_210.25),
            Position("TCS", 3, 3_220.00),
            Position("HDFCBANK", 12, 1_690.75),
        ),
        orders = emptyList(),
    )
}
