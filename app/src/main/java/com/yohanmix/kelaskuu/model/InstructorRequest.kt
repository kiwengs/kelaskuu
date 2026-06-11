package com.yohanmix.kelaskuu.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class InstructorRequest(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val expertise: String = "",
    val address: String = "",
    val experience: String = "",
    val namaBank: String = "",
    val nomorRekening: String = "",
    val certificateUrl: String = "",
    val status: String = "pending", // "pending", "verified", "rejected"
    val adminComment: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
