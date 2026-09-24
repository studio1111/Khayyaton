package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import com.example.ui.KhayyatonApp
import com.example.ui.KhayyatonViewModel
import com.example.ui.KhayyatonViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // App Check must be initialized before any Firebase service is used.
        val firebaseApp = FirebaseApp.initializeApp(applicationContext)
        if (firebaseApp != null) {
            val appCheck = FirebaseAppCheck.getInstance(firebaseApp)
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        }

        com.example.data.firebase.FirebaseService.initialize(applicationContext)
        com.example.data.subscription.SubscriptionManager.initialize(applicationContext)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = WorkshopRepository(
            context = applicationContext,
            database = database,
            orderDao = database.orderDao(),
            paymentDao = database.paymentDao(),
            modelPresetDao = database.modelPresetDao(),
            unitRuleDao = database.unitRuleDao(),
            workshopDao = database.workshopDao()
        )

        val viewModel: KhayyatonViewModel by viewModels {
            KhayyatonViewModelFactory(repository)
        }

        setContent {
            KhayyatonApp(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.data.subscription.SubscriptionManager.disconnect()
    }
}
