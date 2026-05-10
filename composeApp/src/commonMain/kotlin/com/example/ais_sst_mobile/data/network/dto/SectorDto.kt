package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SectorDto(
    val id: Int,
    val title: String,
    val description: String,
    val isParticipant: Boolean = false,
    val isCoordinator: Boolean = false,
    val hasActiveRequest: Boolean = false,
    val participantCount: Int = 0,
    val requestStatus: String? = null,
    val photo: String? = null,
    val isActive: Boolean? = null,
    val coordinatorId: Int? = null,
    val coordinatorName: String? = null,
    val coordinatorSurname: String? = null,
    val coordinatorPatronymic: String? = null,
    val coordinatorFullName: String? = null,
    val coordinatorPhoto: String? = null,
    val coordinatorCourseNumber: Int? = null,
    val coordinatorGroupTitle: String? = null,
    val coordinatorSpecialityTitle: String? = null
)