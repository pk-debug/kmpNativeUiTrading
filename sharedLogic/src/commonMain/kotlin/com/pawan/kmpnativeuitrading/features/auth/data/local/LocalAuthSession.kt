/** Implements device-only demo entry; no real credentials or server sessions exist. */
package com.pawan.kmpnativeuitrading.features.auth.data.local
import com.pawan.kmpnativeuitrading.core.domain.*
import com.pawan.kmpnativeuitrading.features.auth.domain.repository.*
import com.pawan.kmpnativeuitrading.features.auth.domain.usecase.*
class LocalAuthSession(private val repository: SessionRepository) : AuthSession {
    override val initialIdentity get() = repository.loadPhoneNumber()
    override val isRemote = false
    override suspend fun login(name: String, password: String, register: Boolean): String {
        if(register) throw AccountFailure(FailureKind.INVALID_INPUT, "Registration requires API mode.")
        return when(val result = DemoLoginUseCase(repository)(name, password)) {
            is DemoLoginResult.Accepted -> result.phoneNumber
            is DemoLoginResult.Rejected -> throw AccountFailure(FailureKind.INVALID_INPUT, result.message)
        }
    }
    override suspend fun signOut(): String? { repository.clearSession(); return null }
    override suspend fun deleteAccount() { throw AccountFailure(FailureKind.INVALID_INPUT, "No server account exists in offline mode.") }
}
