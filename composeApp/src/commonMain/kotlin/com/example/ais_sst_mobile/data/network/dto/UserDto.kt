package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: Int,
    val name: String,
    val surname: String,
    val patronymic: String? = null,
    val events_count: Int? = null,
    val points_count: Int? = null,
    val rank: Int? = null,
    val dateOfBirth: String,
    val courseNumber: Int,
    val specialityTitle: String,
    val groupTitle: String,
    val studentEmail: String,
    val additionalEmail: String? = null,
    val phoneNumber: String,
    val vkLink: String? = null,
    val photo: String? = null,
    val roleTitle: String
)