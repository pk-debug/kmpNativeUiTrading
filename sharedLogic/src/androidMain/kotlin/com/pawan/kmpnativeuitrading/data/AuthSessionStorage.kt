package com.pawan.kmpnativeuitrading.data

import android.content.Context
import com.pawan.kmpnativeuitrading.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.presentation.AuthViewModel

internal class AndroidSessionRepository(context: Context) : SessionRepository {
    private val preferences = context.applicationContext
        .getSharedPreferences("northstar_demo_account", Context.MODE_PRIVATE)

    override fun loadPhoneNumber(): String? = preferences.getString(PHONE_KEY, null)

    override fun savePhoneNumber(phoneNumber: String) {
        check(preferences.edit().putString(PHONE_KEY, phoneNumber).commit()) {
            "The demo session could not be saved on this device."
        }
    }

    override fun clearSession() {
        check(preferences.edit().remove(PHONE_KEY).commit()) {
            "The demo session could not be cleared from this device."
        }
    }

    private companion object {
        const val PHONE_KEY = "demo_session_phone_v1"
    }
}

fun createAuthViewModel(context: Context): AuthViewModel =
    AuthViewModel(AndroidSessionRepository(context))
