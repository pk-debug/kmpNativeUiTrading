/**
 * Defines Platform.android for the shared native application. Keep platform UI concerns outside domain rules.
 */
package com.pawan.kmpnativeuitrading

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()