package com.pawan.kmpnativeuitrading.domain.model

data class AuthState(
    val phoneNumber: String?,
    val errorMessage: String?,
) {
    val isLoggedIn: Boolean
        get() = phoneNumber != null
}

data class LoginResult(
    val success: Boolean,
    val message: String,
)
