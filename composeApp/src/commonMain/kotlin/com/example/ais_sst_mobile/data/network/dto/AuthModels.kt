package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val type: String = "Bearer",
    val id: Int,
    val email: String,
    val name: String,
    val surname: String,
    val roles: List<String>
)
@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val surname: String,
    val patronymic: String,
    val gender: String,
    val dateOfBirth: String,
    val courseNumber: Int,
    @SerialName("speciality_id") val specialityId: Int,
    @SerialName("group_id") val groupId: Int,
    val studentIdNumber: Int,
    val studentEmail: String,
    val additionalEmail: String? = null,
    val phoneNumber: String,
    val vkLink: String? = null,
    val password: String,
    @SerialName("social_statuses_id") val socialStatusesId: List<Int>,
    val photo: String
)