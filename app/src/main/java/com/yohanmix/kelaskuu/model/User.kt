package com.yohanmix.kelaskuu.model

data class User(
    val uid: String = "",
    val nama: String = "",
    val email: String = "",
    val role: String = "pelajar", // owner, admin, pengajar, pelajar
    val namaBank: String = "",
    val nomorRekening: String = ""
)
