/** Owns observable auth screen state; an injected session encapsulates execution mode. */
package com.pawan.kmpnativeuitrading.features.auth.presentation
import com.pawan.kmpnativeuitrading.core.domain.AccountFailure
import com.pawan.kmpnativeuitrading.features.auth.domain.model.*
import com.pawan.kmpnativeuitrading.features.auth.domain.repository.AuthSession
import kotlinx.coroutines.*

interface AuthStateObserver { fun onAuthStateChanged(state: AuthState) }

class AuthViewModel(
    private val session: AuthSession,
    private val onSignedIn: () -> Unit = {},
    private val onSignedOut: () -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val observers = mutableMapOf<Long, AuthStateObserver>()
    private var nextObserverId = 0L
    private var currentState = AuthState(session.initialIdentity, null, isRemote = session.isRemote)
    val state: AuthState get() = currentState

    fun addObserver(observer: AuthStateObserver): Long {
        val id = nextObserverId++
        observers[id] = observer
        observer.onAuthStateChanged(currentState)
        return id
    }
    fun removeObserver(id: Long) { observers.remove(id) }
    fun close() { scope.cancel(); observers.clear() }
    fun login(phoneNumber: String, password: String) = authenticate(phoneNumber, password, false)
    fun register(username: String, password: String) = authenticate(username, password, true)

    private fun authenticate(name: String, password: String, register: Boolean): LoginResult = perform {
        val identity = session.login(name, password, register)
        onSignedIn()
        AuthState(identity, null, isRemote = session.isRemote)
    }
    fun signOut() { perform {
        val message = session.signOut()
        onSignedOut()
        AuthState(null, message, isRemote = session.isRemote)
    } }
    fun deleteAccount() { perform {
        session.deleteAccount()
        onSignedOut()
        AuthState(null, null, isRemote = session.isRemote)
    } }

    /** Undispatched start preserves immediate local validation without blocking on remote work. */
    private fun perform(action: suspend () -> AuthState): LoginResult {
        if(currentState.isBusy) return LoginResult(false, "Please wait.")
        updateState(currentState.copy(isBusy = true, errorMessage = null))
        var result = LoginResult(true, "Request submitted…")
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try { updateState(action()); result = LoginResult(true, "Completed.") }
            catch(cancelled: CancellationException) { throw cancelled }
            catch(failure: Exception) {
                val message = (failure as? AccountFailure)?.message ?: "Cannot reach the service. Try again."
                updateState(currentState.copy(isBusy = false, errorMessage = message))
                result = LoginResult(false, message)
            }
        }
        return result
    }
    private fun updateState(value: AuthState) {
        currentState = value
        observers.values.toList().forEach { it.onAuthStateChanged(value) }
    }
}
