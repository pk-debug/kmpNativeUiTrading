package com.pawan.kmpnativeuitrading.data

import com.pawan.kmpnativeuitrading.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.presentation.AuthViewModel
import platform.Foundation.NSUserDefaults

private class IOSSessionRepository : SessionRepository {
    override fun loadPhoneNumber(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(PHONE_KEY)

    override fun savePhoneNumber(phoneNumber: String) {
        NSUserDefaults.standardUserDefaults.setObject(phoneNumber, forKey = PHONE_KEY)
    }

    override fun clearSession() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(PHONE_KEY)
    }

    private companion object {
        const val PHONE_KEY = "northstar_demo_session_phone_v1"
    }
}

fun createAuthViewModel(): AuthViewModel = AuthViewModel(IOSSessionRepository())
