/**
 * Implements the HTTPS paper-account protocol, including explicit idempotent retries and safe failure messages. UI reads use memory; retry persistence uses a worker dispatcher.
 */
@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.pawan.kmpnativeuitrading.features.trading.data.remote

import com.pawan.kmpnativeuitrading.features.trading.domain.model.*
import com.pawan.kmpnativeuitrading.features.trading.domain.repository.AccountGateway
import com.pawan.kmpnativeuitrading.features.auth.domain.repository.AuthGateway
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Stores retry instructions, never passwords or bearer tokens. Keys include the account name. */
interface RetryStorage {
    fun read(key: String): String?
    fun write(key: String, value: String?)
}

@Serializable data class Credentials(val username: String, val password: String)
@Serializable data class SessionResponse(val token: String, val expiresAt: Long)
@Serializable data class OrderRequest(val symbol: String, val side: String, val quantity: Int)
@Serializable data class PendingOrder(val key: String, val request: OrderRequest)
@Serializable data class ApiOrder(
    val id: String, val symbol: String, val side: OrderSide, val quantity: Int,
    val price: Double, val createdAt: String,
) {
    fun toDomain() = SimulatedOrder(id, symbol, side, quantity, price, Instant.parse(createdAt).toEpochMilliseconds())
}
@Serializable data class AccountSnapshot(
    val cash: Double, val positions: List<Position>, val orders: List<ApiOrder>,
    val watchlist: List<String> = emptyList(), val currency: String = "INR",
) {
    fun toLedger() = TradingLedger(cash, positions, orders.map(ApiOrder::toDomain), watchlist)
}

class ApiFailure(val status: Int, message: String) : Exception(message)

/** TLS remains enabled. We use deadlines but never automatically retry a money-changing POST. */
fun configureTradingClient(client: HttpClient) = client.config {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
    followRedirects = false // A redirect must not send a bearer token to a different host.
}

/** A session is memory-only. Restarting the app asks the user to sign in again.
 *
 * Before sending an order we save its UUID. A lost response can then be retried
 * with the same UUID instead of accidentally buying twice. Failed orders are
 * never silently queued for execution when the user is away.
 */
class RemoteAccount(
    baseUrl: String,
    private val client: HttpClient,
    private val retries: RetryStorage,
    allowLocalHttp: Boolean = false,
    private val storageDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AccountGateway, AuthGateway {
    private val root = baseUrl.trimEnd('/')
    private var token: String? = null
    private var username: String? = null
    private val json = Json { ignoreUnknownKeys = true }
    init {
        val url = Url(root)
        require(url.protocol == URLProtocol.HTTPS || (allowLocalHttp && url.protocol == URLProtocol.HTTP && url.host in setOf("localhost", "127.0.0.1", "10.0.2.2"))) {
            "The API must use HTTPS. HTTP is allowed only for a debug loopback backend."
        }
        require(url.user.isNullOrEmpty() && url.password.isNullOrEmpty() && url.encodedPath in setOf("", "/") && url.parameters.isEmpty() && url.fragment.isEmpty()) {
            "Use an API origin without credentials, path, query or fragment."
        }
    }
    // Rendering must never read disk or decode JSON. Load once when authenticating.
    var pending: PendingOrder? = null
        private set

    override suspend fun authenticate(name: String, password: String, register: Boolean) {
        val session: SessionResponse = request(if (register) "/api/v2/auth/register" else "/api/v2/auth/login", HttpMethod.Post, Credentials(name, password))
        val normalized = name.lowercase()
        val restored = withContext(storageDispatcher) {
            retries.read("pending:$root:$normalized")?.let { json.decodeFromString<PendingOrder>(it) }
        }
        token = session.token
        username = normalized
        pending = restored
    }

    override val hasPendingOrder: Boolean get() = pending != null
    private fun AccountSnapshot.domainLedger(): TradingLedger {
        check(currency == "INR") { "This application requires INR accounts." }
        return toLedger()
    }
    override suspend fun snapshotLedger() = snapshot().domainLedger()
    override suspend fun submitOrder(symbol: String, isBuy: Boolean, quantity: Int) =
        submit(OrderRequest(symbol, if (isBuy) "BUY" else "SELL", quantity)).domainLedger()
    override suspend fun retryOrder() = retry().domainLedger()
    override suspend fun setWatchlist(symbol: String, enabled: Boolean) = watch(symbol, enabled).domainLedger()

    suspend fun snapshot(): AccountSnapshot = request("/api/v2/snapshot")
    override suspend fun assets(): List<MarketAsset> = request("/api/v2/assets")
    override suspend fun indexes(): List<MarketIndex> = request("/api/v2/indexes")

    suspend fun submit(request: OrderRequest): AccountSnapshot {
        check(pending == null) { "Resolve the previous order before placing another." }
        val order = PendingOrder(Uuid.random().toString(), request)
        savePending(order)
        return sendPending(order)
    }

    suspend fun retry(): AccountSnapshot = sendPending(checkNotNull(pending))

    private suspend fun sendPending(order: PendingOrder): AccountSnapshot {
        try {
            val response = client.post("$root/api/v2/orders") {
                bearerAuth(checkNotNull(token))
                contentType(ContentType.Application.Json)
                header("Idempotency-Key", order.key)
                setBody(order.request)
            }
            checkResponse(response.status.value)
            // Keep the retry record until the UI can read the server's committed state.
            val result = snapshot()
            savePending(null)
            return result
        } catch (failure: ApiFailure) {
            if (failure.status in setOf(400, 404, 409, 422)) savePending(null)
            throw failure
        }
    }

    suspend fun watch(symbol: String, enabled: Boolean): AccountSnapshot {
        request<Unit>("/api/v2/watchlist/$symbol", if (enabled) HttpMethod.Put else HttpMethod.Delete)
        return snapshot()
    }

    override suspend fun deleteAccount() { request<Unit>("/api/v2/account", HttpMethod.Delete); savePending(null); clearSession() }
    override suspend fun revokeSession() { request<Unit>("/api/v2/auth/logout", HttpMethod.Post) }
    override fun clearSession() { token = null; username = null; pending = null }
    fun close() { clearSession(); client.close() }

    private suspend fun savePending(order: PendingOrder?) {
        val key = "pending:$root:${checkNotNull(username)}"
        withContext(storageDispatcher) {
            retries.write(key, order?.let { json.encodeToString(it) })
        }
        pending = order
    }

    private suspend inline fun <reified T> request(path: String, method: HttpMethod = HttpMethod.Get, body: Credentials? = null): T {
        val response = client.request("$root$path") {
            this.method = method
            token?.let { bearerAuth(it) }
            if (body != null) { contentType(ContentType.Application.Json); setBody(body) }
        }
        checkResponse(response.status.value)
        return if (T::class == Unit::class) Unit as T else response.body()
    }

    private fun checkResponse(status: Int) {
        if (status in 200..299) return
        // Do not display raw server bodies: proxies can return HTML, tokens or diagnostics.
        throw ApiFailure(status, when (status) {
            401 -> "Your session expired. Sign out and sign in again."
            409 -> "The request conflicts with your account. Check cash, holdings or username."
            429 -> "Too many requests. Wait before trying again."
            in 400..499 -> "Check your input and try again."
            else -> "The service is unavailable. Your order may still have completed; retry the same order."
        })
    }
}
