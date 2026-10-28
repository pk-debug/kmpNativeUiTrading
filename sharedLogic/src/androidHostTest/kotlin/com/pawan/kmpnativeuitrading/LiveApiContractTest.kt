/**
 * Verifies LiveApiContract behavior with repeatable assertions. Test fixtures are practice data and do not authorize real trading.
 */
package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.features.trading.data.remote.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.*

/** Opt-in real HTTP contract check. No production URL or credentials are used. */
class LiveApiContractTest {
    @Test fun nativeClientCompletesAnIsolatedPaperAccountJourney() = runBlocking {
        val origin = System.getenv("NORTHSTAR_TEST_API")
        assumeTrue("Set NORTHSTAR_TEST_API to run the loopback API contract test", !origin.isNullOrBlank())
        require(Url(checkNotNull(origin)).host in setOf("127.0.0.1", "localhost")) { "Contract tests require a loopback server." }
        val storage = object : RetryStorage {
            val values = mutableMapOf<String, String>()
            override fun read(key: String) = values[key]
            override fun write(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
        }
        val api = RemoteAccount(checkNotNull(origin), configureTradingClient(HttpClient(OkHttp)), storage, true)
        try {
            api.authenticate("contract-${System.nanoTime()}", "contract-test-passphrase", true)
            assertEquals(7, api.assets().size)
            assertEquals(3, api.indexes().size)
            assertEquals(1_000_000.0, api.snapshot().cash)
            val bought = api.submit(OrderRequest("RELIANCE", "BUY", 1))
            assertEquals(998_715.50, bought.cash)
            assertEquals(1, bought.positions.single().quantity)
            assertEquals(1, bought.orders.size)
            assertEquals(listOf("TCS"), api.watch("TCS", true).watchlist)
            val sold = api.submit(OrderRequest("RELIANCE", "SELL", 1))
            assertEquals(1_000_000.0, sold.cash)
            assertEquals(emptyList(), sold.positions)
            assertEquals(2, sold.toLedger().orders.size)
            api.deleteAccount()
            assertEquals(401, assertFailsWith<ApiFailure> { api.snapshot() }.status)
        } finally { api.close() }
    }
}
