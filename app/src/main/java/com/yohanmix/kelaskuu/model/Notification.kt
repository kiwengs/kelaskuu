package com.yohanmix.kelaskuu.model

import com.google.firebase.Timestamp
import java.util.Date

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "info", // "payment", "registration", "system"
    val timestamp: Date? = null,
    val isRead: Boolean = false
)
