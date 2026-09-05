package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.WorkshopRepository
import com.example.ui.SheetOnApp
import com.example.ui.SheetOnViewModel
import com.example.ui.SheetOnViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.data.firebase.FirebaseService.initialize(applicationContext)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = WorkshopRepository(
            context = applicationContext,
            orderDao = database.orderDao(),
            paymentDao = database.paymentDao(),
            modelPresetDao = database.modelPresetDao(),
            unitRuleDao = database.unitRuleDao(),
            workshopDao = database.workshopDao()
        )

        val viewModel: SheetOnViewModel by viewModels {
            SheetOnViewModelFactory(repository)
        }

        setContent {
            SheetOnApp(viewModel = viewModel)
        }
    }
}
