package com.pawan.kmpnativeuitrading.domain.usecase

import com.pawan.kmpnativeuitrading.domain.repository.SessionRepository

class DemoLoginUseCase(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(phoneNumber: String, password: String): DemoLoginResult {
        val normalizedPhone = phoneNumber.trim()
        if (!normalizedPhone.all { it in '0'..'9' || it in "+ ()-" }) {
            return DemoLoginResult.Rejected("Enter a valid phone number.")
        }

        val digitCount = normalizedPhone.count { it in '0'..'9' }
        if (digitCount !in 10..15) {
            return DemoLoginResult.Rejected("Phone number must contain 10 to 15 digits.")
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return DemoLoginResult.Rejected("Password must be at least 8 characters.")
        }

        sessionRepository.savePhoneNumber(normalizedPhone)
        return DemoLoginResult.Accepted(normalizedPhone)
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}

sealed interface DemoLoginResult {
    data class Accepted(val phoneNumber: String) : DemoLoginResult
    data class Rejected(val message: String) : DemoLoginResult
}
