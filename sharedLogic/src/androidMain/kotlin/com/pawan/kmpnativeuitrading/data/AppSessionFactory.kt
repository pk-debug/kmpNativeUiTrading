package com.pawan.kmpnativeuitrading.data

import android.content.Context
import com.pawan.kmpnativeuitrading.network.*
import com.pawan.kmpnativeuitrading.presentation.AppSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/** Build-time API configuration keeps test servers out of release artifacts. */
fun createAppSession(context: Context, apiOrigin: String = "", debug: Boolean = false): AppSession {
    val prefs = context.applicationContext.getSharedPreferences("northstar_order_retries", Context.MODE_PRIVATE)
    val storage = object : RetryStorage {
        override fun read(key: String) = prefs.getString(key, null)
        override fun write(key: String, value: String?) {
            check(prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.commit()) {
                "Could not save the order retry key. The order was not sent."
            }
        }
    }
    val remote = apiOrigin.takeIf { it.isNotBlank() }?.let {
        RemoteAccount(it, configureTradingClient(HttpClient(OkHttp)), storage, allowLocalHttp = debug)
    }
    return AppSession(DefaultTradingRepository(AndroidLedgerStorage(context)), AndroidSessionRepository(context), remote)
}
