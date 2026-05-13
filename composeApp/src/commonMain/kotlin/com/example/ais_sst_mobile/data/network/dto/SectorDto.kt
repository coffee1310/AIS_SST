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
    val requestStatus: String? = null,
    val participantCount: Int = 0,
    val photo: String? = null,

    val coordinators: List<CoordinatorDto> = emptyList()
)

@Serializable
data class CoordinatorDto(
    val id: Int,
    val studentId: Int,
    val studentName: String,
    val studentSurname: String,
    val studentPatronymic: String? = null,
    val studentEmail: String? = null,
    val studentPhoto: String? = null,
    val studentCourseNumber: Int? = null,
    val studentGroupTitle: String? = null,
    val studentSpecialityTitle: String? = null,
    val entryDate: String? = null,
    val status: String? = null,
    val isCoordinator: Boolean = false
)