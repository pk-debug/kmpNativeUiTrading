package com.pawan.kmpnativeuitrading.data

import com.pawan.kmpnativeuitrading.network.*
import com.pawan.kmpnativeuitrading.presentation.AppSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

/** Tokens remain in memory; UserDefaults stores only the non-secret order retry record. */
fun createAppSession(): AppSession {
    val storage = object : RetryStorage {
        override fun read(key: String) = NSUserDefaults.standardUserDefaults.stringForKey("northstar.$key")
        override fun write(key: String, value: String?) {
            if (value == null) NSUserDefaults.standardUserDefaults.removeObjectForKey("northstar.$key")
            else NSUserDefaults.standardUserDefaults.setObject(value, forKey = "northstar.$key")
        }
    }
    val origin = NSBundle.mainBundle.objectForInfoDictionaryKey("NorthstarAPIOrigin") as? String ?: ""
    val remote = origin.takeIf { it.isNotBlank() && !it.startsWith("$(") }?.let {
        RemoteAccount(it, configureTradingClient(HttpClient(Darwin)), storage)
    }
    return AppSession(DefaultTradingRepository(IOSLedgerStorage()), IOSSessionRepository(), remote)
}
