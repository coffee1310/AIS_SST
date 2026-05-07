package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SectorDto(
    val id: Int,
    val title: String,
    val description: String,
    val isParticipant: Boolean,
    val isCoordinator: Boolean,
    val hasActiveRequest: Boolean,
    val requestStatus: String? = null,
    val participantCount: Int,
    val photo: String? = null,
    val coordinatorName: String? = null,
    val coordinatorSurname: String? = null,
    val coordinatorPatronymic: String? = null,
    val coordinatorFullName: String? = null,
    val coordinatorPhoto: String? = null
)