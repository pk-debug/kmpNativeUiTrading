/**
 * Defines Platform for the shared native application. Keep platform UI concerns outside domain rules.
 */
package com.pawan.kmpnativeuitrading

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform