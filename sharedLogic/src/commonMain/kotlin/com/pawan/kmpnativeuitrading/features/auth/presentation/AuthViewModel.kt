/**
 * Owns sign-in, registration and sign-out state. Credentials go to the account service and are never saved in local demo storage.
 */
package com.pawan.kmpnativeuitrading.features.auth.presentation

import com.pawan.kmpnativeuitrading.features.auth.domain.model.AuthState
import com.pawan.kmpnativeuitrading.features.auth.domain.model.LoginResult
import com.pawan.kmpnativeuitrading.features.auth.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.features.auth.domain.usecase.DemoLoginResult
import com.pawan.kmpnativeuitrading.features.auth.domain.usecase.DemoLoginUseCase

import com.pawan.kmpnativeuitrading.features.trading.data.remote.RemoteAccount
import com.pawan.kmpnativeuitrading.features.trading.data.remote.ApiFailure
import kotlinx.coroutines.*

interface AuthStateObserver {
    fun onAuthStateChanged(state: AuthState)
}

class AuthViewModel(
    private val sessionRepository: SessionRepository,
    private val remote: com.pawan.kmpnativeuitrading.features.auth.domain.repository.AuthGateway? = null,
    private val onSignedIn: () -> Unit = {},
    private val onSignedOut: () -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val demoLogin = DemoLoginUseCase(sessionRepository)
    private val observers = mutableMapOf<Long, AuthStateObserver>()
    private var nextObserverId = 0L
    private var currentState = AuthState(
        phoneNumber = if (remote == null) sessionRepository.loadPhoneNumber() else null,
        isRemote = remote != null,
        errorMessage = null,
    )

    val state: AuthState
        get() = currentState

    fun addObserver(observer: AuthStateObserver): Long {
        val id = nextObserverId++
        observers[id] = observer
        observer.onAuthStateChanged(currentState)
        return id
    }

    fun close() { scope.cancel(); observers.clear() }

    fun removeObserver(id: Long) {
        observers.remove(id)
    }

    fun login(phoneNumber: String, password: String): LoginResult {
        if (remote != null) return authenticate(phoneNumber, password, false)
        return when (val result = demoLogin(phoneNumber, password)) {
            is DemoLoginResult.Accepted -> {
                updateState(AuthState(result.phoneNumber, null))
                LoginResult(success = true, message = "login successful.")
            }
            is DemoLoginResult.Rejected -> {
                updateState(currentState.copy(errorMessage = result.message))
                LoginResult(success = false, message = result.message)
            }
        }
    }

    fun register(username: String, password: String): LoginResult = authenticate(username, password, true)

    private fun authenticate(username: String, password: String, register: Boolean): LoginResult {
        if (remote == null) return LoginResult(false, "Registration requires API mode.")
        if (currentState.isBusy) return LoginResult(false, "Please wait.")
        if (!Regex("[a-zA-Z0-9_.-]{3,40}").matches(username) || password.length !in 12..128) {
            val message = "Use a 3–40 character username and a 12–128 character password."
            updateState(currentState.copy(errorMessage = message))
            return LoginResult(false, message)
        }
        updateState(currentState.copy(isBusy = true, errorMessage = null))
        scope.launch {
            try {
                remote.authenticate(username, password, register)
                updateState(AuthState(username.lowercase(), null, isRemote = true))
                onSignedIn()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                updateState(AuthState(null, (failure as? ApiFailure)?.message ?: "Cannot reach the service. Try again.", isRemote = true))
            }
        }
        return LoginResult(true, "Signing in…")
    }

    fun deleteAccount() {
        if (remote == null || currentState.isBusy) return
        updateState(currentState.copy(isBusy = true))
        scope.launch {
            try {
                remote.deleteAccount()
                onSignedOut()
                updateState(AuthState(null, null, isRemote = true))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                updateState(currentState.copy(isBusy = false, errorMessage = "Account deletion failed. Check your connection and try again."))
            }
        }
    }

    fun signOut() {
        if (remote != null) {
            if (currentState.isBusy) return
            updateState(currentState.copy(isBusy = true))
            scope.launch {
                try {
                    remote.revokeSession()
                    remote.clearSession()
                    onSignedOut()
                    updateState(AuthState(null, null, isRemote = true))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) {
                    remote.clearSession()
                    onSignedOut()
                    val message = if (failure is ApiFailure && failure.status == 401) null
                        else "Signed out on this phone. Server revocation could not be confirmed; the old session expires within 8 hours."
                    updateState(AuthState(null, message, isRemote = true))
                }
            }
            return
        }
        sessionRepository.clearSession()
        updateState(AuthState(phoneNumber = null, errorMessage = null))
    }

    private fun updateState(newState: AuthState) {
        currentState = newState
        observers.values.toList().forEach { it.onAuthStateChanged(currentState) }
    }
}
