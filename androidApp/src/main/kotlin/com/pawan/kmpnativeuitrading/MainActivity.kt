/**
 * Android lifecycle entry point. A retained ViewModel owns the app session across configuration changes.
 */
package com.pawan.kmpnativeuitrading

import com.pawan.kmpnativeuitrading.features.auth.ui.NorthstarApp
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pawan.kmpnativeuitrading.core.platform.createAppSession
import com.pawan.kmpnativeuitrading.core.session.AppSession

class MainActivity : ComponentActivity() {
    private var frameDiagnostics: com.pawan.kmpnativeuitrading.core.performance.FrameDiagnostics? = null

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        if (BuildConfig.DEBUG) frameDiagnostics = com.pawan.kmpnativeuitrading.core.performance.FrameDiagnostics(window, windowManager.defaultDisplay.refreshRate)
    }

    override fun onStop() {
        frameDiagnostics?.close()
        frameDiagnostics = null
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The ViewModel survives rotation; the Activity and its views do not.
        val holder: SessionHolder = ViewModelProvider(this, SessionFactory(applicationContext))[SessionHolder::class.java]
        setContent { NorthstarApp(holder.session.auth, holder.session.trading) }
    }
}

private class SessionHolder(val session: AppSession) : ViewModel() {
    override fun onCleared() { session.close() }
}

private class SessionFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == SessionHolder::class.java)
        return SessionHolder(createAppSession(context, BuildConfig.API_ORIGIN, BuildConfig.DEBUG)) as T
    }
}
