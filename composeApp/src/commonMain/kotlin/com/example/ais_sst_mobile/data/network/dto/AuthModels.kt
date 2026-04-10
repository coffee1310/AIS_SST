package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val type: String = "Bearer",
    val id: Int,
    val email: String,
    val name: String,
    val surname: String,
    val roles: List<String>
)

@Serializable
data class RegisterRequest(
    val name: String,
    val surname: String,
    val patronymic: String? = null,
    val gender: String,
    val dateOfBirth: String,
    val studentEmail: String,
    val phoneNumber: String,
    val password: String
)
