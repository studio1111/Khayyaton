package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import com.example.ui.KhayyatonApp
import com.example.ui.KhayyatonViewModel
import com.example.ui.KhayyatonViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase services first
        com.example.data.firebase.FirebaseService.initialize(applicationContext)

        // Install App Check safely
        try {
            val app = FirebaseApp.getInstance()
            AppCheckProviderInstaller.install(FirebaseAppCheck.getInstance(app))
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "App Check initialization skipped: ${e.message}")
        }

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
