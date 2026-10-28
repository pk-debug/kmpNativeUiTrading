/**
 * Verifies TradingApi behavior with repeatable assertions. Test fixtures are practice data and do not authorize real trading.
 */
package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.features.trading.broker.ExecutionMode
import com.pawan.kmpnativeuitrading.features.trading.broker.ExecutionPolicy
import com.pawan.kmpnativeuitrading.features.trading.data.remote.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TradingApiTest {
    private class MemoryRetries : RetryStorage {
        var reads = 0
        val values = mutableMapOf<String, String>()
        override fun read(key: String): String? { reads++; return values[key] }
        override fun write(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
    }

    @Test fun liveExecutionCannotBeEnabledByAnApiKey() {
        ExecutionPolicy().requireSupported(ExecutionMode.PAPER)
        assertFailsWith<IllegalArgumentException> { ExecutionPolicy().requireSupported(ExecutionMode.LIVE) }
    }

    @Test fun cleartextIsLimitedToExplicitLocalDebugOrigins() {
        val client = configureTradingClient(HttpClient(MockEngine { respond("{}") }))
        assertFailsWith<IllegalArgumentException> { RemoteAccount("http://example.com", client, MemoryRetries(), true) }
        assertFailsWith<IllegalArgumentException> { RemoteAccount("http://localhost:8000", client, MemoryRetries()) }
        assertFailsWith<IllegalArgumentException> { RemoteAccount("https://user:pass@example.com", client, MemoryRetries()) }
        RemoteAccount("http://10.0.2.2:8000", client, MemoryRetries(), true)
        client.close()
    }

    @Test fun aLostResponseSurvivesRestartAndRetriesTheSameOrderKey() = runTest {
        val storage = MemoryRetries()
        val keys = mutableListOf<String>()
        var loseResponse = true
        val engine = MockEngine { request ->
            val headers = headersOf(HttpHeaders.ContentType, "application/json")
            when (request.url.encodedPath) {
                "/api/v2/auth/login" -> respond("""{"token":"memory-only-token","expiresAt":9999999999}""", headers = headers)
                "/api/v2/orders" -> {
                    assertEquals("Bearer memory-only-token", request.headers[HttpHeaders.Authorization])
                    keys += checkNotNull(request.headers["Idempotency-Key"])
                    if (loseResponse) { loseResponse = false; error("The server committed, but the response was lost") }
                    respond("""{"ok":true}""", headers = headers)
                }
                "/api/v2/snapshot" -> respond("""{"cash":998715.5,"positions":[],"orders":[],"watchlist":["TCS"]}""", headers = headers)
                else -> error("Unexpected endpoint ${request.url}")
            }
        }
        val client = configureTradingClient(HttpClient(engine))
        val first = RemoteAccount("https://example.com", client, storage)
        first.authenticate("alice", "test-passphrase", false)
        assertFailsWith<IllegalStateException> { first.submit(OrderRequest("RELIANCE", "BUY", 1)) }
        assertNotNull(first.pending)
        val readsAfterLogin = storage.reads
        repeat(100) { assertNotNull(first.pending) }
        assertEquals(readsAfterLogin, storage.reads, "Rendering retry state must not access disk")
        assertFalse(storage.values.values.any { "memory-only-token" in it || "test-passphrase" in it })
        val restarted = RemoteAccount("https://example.com", client, storage)
        restarted.authenticate("bob", "test-passphrase", false)
        assertNull(restarted.pending) // Another user never sees Alice's retry.
        restarted.authenticate("alice", "test-passphrase", false)
        val result = restarted.retry()
        assertEquals(998715.5, result.cash)
        assertEquals(listOf("TCS"), result.watchlist)
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
        assertNull(restarted.pending)
        client.close()
    }

    @Test fun aDefinitiveRejectionClearsTheRetryRecord() = runTest {
        val storage = MemoryRetries()
        val client = configureTradingClient(HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("login")) respond("""{"token":"token","expiresAt":99999999}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            else respond("secret diagnostic must never reach the UI", HttpStatusCode.Conflict)
        }))
        val account = RemoteAccount("https://example.com", client, storage)
        account.authenticate("alice", "password-12345", false)
        val failure = assertFailsWith<ApiFailure> { account.submit(OrderRequest("TCS", "BUY", 1000000)) }
        assertEquals(409, failure.status)
        assertFalse(failure.message.orEmpty().contains("secret"))
        assertNull(account.pending)
        client.close()
    }
}
