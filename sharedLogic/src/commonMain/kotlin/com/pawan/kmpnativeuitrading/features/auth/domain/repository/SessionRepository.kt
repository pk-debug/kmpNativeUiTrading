/**
 * Defines SessionRepository for the shared native application. Keep platform UI concerns outside domain rules.
 */
package com.pawan.kmpnativeuitrading.features.auth.domain.repository

interface SessionRepository {
    fun loadPhoneNumber(): String?
    fun savePhoneNumber(phoneNumber: String)
    fun clearSession()
}
