package com.example.ais_sst_mobile.domain.model

data class User(
    val id: Int,
    val name: String,
    val surname: String,
    val patronymic: String?,
    val eventsCount: Int?,
    val pointsCount: Int?,
    val rank: Int?,
    val dateOfBirth: String,
    val courseNumber: Int,
    val specialityTitle: String,
    val groupTitle: String,
    val studentEmail: String,
    val additionalEmail: String?,
    val phoneNumber: String,
    val vkLink: String?,
    val photo: String?,
    val roleTitle: String,
    val gender: String?,
    val socialStatuses: List<String>?
)