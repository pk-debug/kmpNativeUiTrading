package com.pawan.kmpnativeuitrading.domain.repository

interface SessionRepository {
    fun loadPhoneNumber(): String?
    fun savePhoneNumber(phoneNumber: String)
    fun clearSession()
}
