package com.pawan.kmpnativeuitrading

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform