package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val surname: String,
    val patronymic: String? = null,
    val gender: String, // "Мужчина", "Женщина"
    val dateOfBirth: String, // Формат "YYYY-MM-DD"
    val studentEmail: String,
    val phoneNumber: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val role: String // Бэкенд должен возвращать роль (например, "Активист", "Председатель") для маршрутизации
)