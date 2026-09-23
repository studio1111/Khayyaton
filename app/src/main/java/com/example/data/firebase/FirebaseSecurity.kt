package com.example.data.firebase

import android.content.Context
import com.example.BuildConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object FirebaseSecurity {
    fun initialize(context: Context) {
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        } catch (e: Exception) {
            android.util.Log.w(
                "FirebaseSecurity",
                "Firebase App Check unavailable because Firebase is not configured yet: ${e.message}"
            )
        }
    }
}
