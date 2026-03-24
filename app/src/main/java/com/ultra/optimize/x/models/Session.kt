package com.ultra.optimize.x.models

import com.google.firebase.Timestamp

data class Session(
    val id: String = "",
    val deviceName: String = "",
    val androidVersion: String = "",
    val lastActive: Timestamp? = null,
    val status: String = "Active"
)
