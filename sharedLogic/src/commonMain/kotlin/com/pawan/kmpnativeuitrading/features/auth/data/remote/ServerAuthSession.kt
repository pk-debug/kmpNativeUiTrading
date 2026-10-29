/** Owns server identity validation and revocation policy, independent of screen state. */
package com.pawan.kmpnativeuitrading.features.auth.data.remote
import com.pawan.kmpnativeuitrading.core.domain.*
import com.pawan.kmpnativeuitrading.features.auth.domain.repository.*
import kotlinx.coroutines.CancellationException
class ServerAuthSession(private val gateway: AuthGateway) : AuthSession {
    override val initialIdentity: String? = null
    override val isRemote = true
    override suspend fun login(name: String, password: String, register: Boolean): String {
        if(!Regex("[a-zA-Z0-9_.-]{3,40}").matches(name) || password.length !in 12..128)
            throw AccountFailure(FailureKind.INVALID_INPUT, "Use a 3–40 character username and a 12–128 character password.")
        gateway.authenticate(name, password, register)
        return name.lowercase()
    }
    override suspend fun signOut(): String? {
        try { gateway.revokeSession() }
        catch(cancelled: CancellationException) { throw cancelled }
        catch(failure: Exception) {
            return if(failure is AccountFailure && failure.kind == FailureKind.UNAUTHENTICATED) null
                else "Signed out on this phone. Server revocation could not be confirmed; the old session expires within 8 hours."
        } finally { gateway.clearSession() }
        return null
    }
    override suspend fun deleteAccount() = gateway.deleteAccount()
}
