package com.example.data.firebase

data class FirebaseUserDto(
    val uid: String,
    val email: String,
    val displayName: String? = null
)

data class CloudSyncResult(
    val success: Boolean,
    val ordersCount: Int = 0,
    val paymentsCount: Int = 0,
    val presetsCount: Int = 0,
    val unitRulesCount: Int = 0,
    val workshopsCount: Int = 0,
    val errorMessage: String? = null
)
