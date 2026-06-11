package com.yohanmix.kelaskuu.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Feedback(
    val id: String = "",
    val userId: String = "",
    val email: String = "",
    val masukan: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
