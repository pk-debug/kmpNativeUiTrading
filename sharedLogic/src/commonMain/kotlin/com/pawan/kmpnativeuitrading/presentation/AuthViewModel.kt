package com.pawan.kmpnativeuitrading.presentation

import com.pawan.kmpnativeuitrading.domain.model.AuthState
import com.pawan.kmpnativeuitrading.domain.model.LoginResult
import com.pawan.kmpnativeuitrading.domain.repository.SessionRepository
import com.pawan.kmpnativeuitrading.domain.usecase.DemoLoginResult
import com.pawan.kmpnativeuitrading.domain.usecase.DemoLoginUseCase

interface AuthStateObserver {
    fun onAuthStateChanged(state: AuthState)
}

class AuthViewModel(
    private val sessionRepository: SessionRepository,
) {
    private val demoLogin = DemoLoginUseCase(sessionRepository)
    private val observers = mutableMapOf<Long, AuthStateObserver>()
    private var nextObserverId = 0L
    private var currentState = AuthState(
        phoneNumber = sessionRepository.loadPhoneNumber(),
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

    fun removeObserver(id: Long) {
        observers.remove(id)
    }

    fun login(phoneNumber: String, password: String): LoginResult {
        return when (val result = demoLogin(phoneNumber, password)) {
            is DemoLoginResult.Accepted -> {
                updateState(AuthState(result.phoneNumber, null))
                LoginResult(true, "Demo login successful.")
            }
            is DemoLoginResult.Rejected -> {
                updateState(currentState.copy(errorMessage = result.message))
                LoginResult(false, result.message)
            }
        }
    }

    fun signOut() {
        sessionRepository.clearSession()
        updateState(AuthState(phoneNumber = null, errorMessage = null))
    }

    private fun updateState(newState: AuthState) {
        currentState = newState
        observers.values.toList().forEach { it.onAuthStateChanged(currentState) }
    }
}
