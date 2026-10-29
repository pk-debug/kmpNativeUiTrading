/** Encapsulates offline versus server authentication behind one behavioral contract. */
package com.pawan.kmpnativeuitrading.features.auth.domain.repository
interface AuthSession {
    val initialIdentity: String?
    val isRemote: Boolean
    suspend fun login(name: String, password: String, register: Boolean): String
    suspend fun signOut(): String?
    suspend fun deleteAccount()
}
