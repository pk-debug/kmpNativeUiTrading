/** Defines account entry and revocation without depending on HTTP or native storage. */
package com.pawan.kmpnativeuitrading.features.auth.domain.repository

interface AuthGateway {
    suspend fun authenticate(name: String, password: String, register: Boolean)
    suspend fun deleteAccount()
    suspend fun revokeSession()
    fun clearSession()
}
