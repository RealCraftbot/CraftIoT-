package com.example.data.model

data class User(
    val email: String,
    val fullName: String,
    val role: String = "Enterprise Administrator",
    val organization: String = "Craft Innovations",
    val token: String
)
