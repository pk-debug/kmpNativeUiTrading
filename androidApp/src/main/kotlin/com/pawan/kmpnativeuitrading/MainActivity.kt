package com.pawan.kmpnativeuitrading

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pawan.kmpnativeuitrading.data.createAppSession
import com.pawan.kmpnativeuitrading.presentation.AppSession

class MainActivity : ComponentActivity() {
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
