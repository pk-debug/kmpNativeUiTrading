/** Persists account/origin-scoped retry intent without owning HTTP, tokens or UI state. */
package com.pawan.kmpnativeuitrading.features.trading.data.remote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class PendingOrderJournal(private val origin: String, private val storage: RetryStorage, private val dispatcher: CoroutineDispatcher) {
    private val json = Json { ignoreUnknownKeys = true }
    private var owner: String? = null
    var pending: PendingOrder? = null
        private set
    suspend fun restore(identity: String) {
        val restored = withContext(dispatcher) {
            storage.read("pending:$origin:$identity")?.let { json.decodeFromString<PendingOrder>(it) }
        }
        owner = identity
        pending = restored
    }
    suspend fun persist(order: PendingOrder?) {
        val key = "pending:$origin:${checkNotNull(owner)}"
        withContext(dispatcher) { storage.write(key, order?.let { json.encodeToString(it) }) }
        pending = order
    }
    fun clearMemory() { owner = null; pending = null }
}
