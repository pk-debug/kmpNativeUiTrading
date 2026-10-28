/**
 * Defines AuthState for the shared native application. Keep platform UI concerns outside domain rules.
 */
package com.pawan.kmpnativeuitrading.features.auth.domain.model

data class AuthState(
    val phoneNumber: String?,
    val errorMessage: String?,
    val isBusy: Boolean = false,
    val isRemote: Boolean = false,
) {
    val isLoggedIn: Boolean
        get() = phoneNumber != null
}

data class LoginResult(
    val success: Boolean,
    val message: String,
)
