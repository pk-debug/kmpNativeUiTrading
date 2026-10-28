/**
 * Implements AuthSessionStorage using native device persistence. No broker secret or password belongs in this storage.
 */
package com.pawan.kmpnativeuitrading.core.platform

import com.pawan.kmpnativeuitrading.features.auth.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.features.auth.presentation.AuthViewModel
import platform.Foundation.NSUserDefaults

internal class IOSSessionRepository : SessionRepository {
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
