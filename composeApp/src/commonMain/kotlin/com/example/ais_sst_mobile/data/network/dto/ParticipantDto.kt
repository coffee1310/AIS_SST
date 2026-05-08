package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ParticipantResponseDto(
    val content: List<ParticipantDto>,
    val totalElements: Int,
    val totalPages: Int,
    val size: Int,
    val number: Int
)

@Serializable
data class ParticipantDto(
    val id: Int,
    val studentId: Int,
    val studentName: String,
    val studentSurname: String,
    val studentPatronymic: String? = null,
    val studentEmail: String,
    val studentPhoto: String? = null,
    val studentCourseNumber: Int,
    val studentGroupTitle: String,
    val studentSpecialityTitle: String,
    val entryDate: String,
    val status: String,
    val isCoordinator: Boolean
)