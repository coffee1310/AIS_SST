package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class OrganizerApplicationDto(
    val id: Int,
    val userId: Int,
    val userName: String,
    val userSurname: String,
    val userEmail: String,
    val eventId: Int,
    val eventTitle: String,
    val createdAt: String,
    val status: String
)