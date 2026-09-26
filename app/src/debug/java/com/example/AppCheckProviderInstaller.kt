package com.example

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckProviderInstaller {
    fun install(appCheck: FirebaseAppCheck) {
        try {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } catch (e: Exception) {
            Log.w("AppCheckProvider", "Failed to install debug AppCheck provider: ${e.message}")
        }
    }
}
