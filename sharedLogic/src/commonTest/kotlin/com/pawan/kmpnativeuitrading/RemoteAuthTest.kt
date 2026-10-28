/**
 * Verifies RemoteAuth behavior with repeatable assertions. Test fixtures are practice data and do not authorize real trading.
 */
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.features.auth.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.features.trading.data.remote.*
import com.pawan.kmpnativeuitrading.features.auth.presentation.AuthViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.*
import kotlin.test.*

class RemoteAuthTest {
    @Test fun anUnavailableLogoutStillClearsLocalAccessAndReportsUnconfirmedRevocation() = runTest {
        val cache = object : RetryStorage {
            override fun read(key: String): String? = null
            override fun write(key: String, value: String?) = Unit
        }
        val sessions = object : SessionRepository {
            override fun loadPhoneNumber(): String? = null
            override fun savePhoneNumber(phoneNumber: String) { error("API credentials must not enter local demo storage") }
            override fun clearSession() = Unit
        }
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { request ->
            if (request.url.encodedPath.endsWith("login")) respond("""{"token":"in-memory","expiresAt":9999999999}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            else respond("unavailable", HttpStatusCode.ServiceUnavailable)
            }
        })
        val client = configureTradingClient(HttpClient(engine))
        val remote = RemoteAccount("https://example.com", client, cache, storageDispatcher = StandardTestDispatcher(testScheduler))
        var cleared = false
        val auth = AuthViewModel(sessions, remote, onSignedOut = { cleared = true }, scope = this)
        auth.login("alice", "long-passphrase-123")
        advanceUntilIdle()
        assertTrue(auth.state.isLoggedIn)
        auth.signOut()
        advanceUntilIdle()
        assertFalse(auth.state.isLoggedIn)
        assertFalse(auth.state.isBusy)
        assertTrue(cleared)
        assertTrue(auth.state.errorMessage.orEmpty().contains("revocation could not be confirmed"))
        remote.close()
    }
}
