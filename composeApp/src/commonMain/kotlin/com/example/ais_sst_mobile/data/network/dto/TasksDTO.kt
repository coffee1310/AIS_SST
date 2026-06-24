package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RoleApplicationDto(
    val id: Int,
    val studentId: Int? = null,
    val studentName: String? = null,
    val studentSurname: String? = null,
    val studentPatronymic: String? = null,
    val studentEmail: String? = null,
    val sectorParticipantId: Int? = null,
    val sectorTitle: String? = null,
    val eventRoleId: Int,
    val eventRoleTitle: String,
    val eventId: Int,
    val eventTitle: String,
    val eventPhoto: String? = null, // Задел на будущее, чтобы выводить фото
    val roleDeadline: String? = null, // Задел на будущее для вывода дедлайна
    val isReserve: Boolean? = null,
    val status: String,
    val rejectionReason: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val description: String? = null
)

@Serializable
data class PagedRoleApplicationsResponse(
    val content: List<RoleApplicationDto>
)

@Serializable
data class MyEventParticipantDto(
    val id: Int,
    val eventId: Int,
    val eventTitle: String,
    val eventDescription: String? = null,
    val eventPhoto: String? = null, // Задел на будущее
    val eventDateOfEvent: String? = null,
    val eventStartTime: String? = null,
    val eventEndTime: String? = null,
    val eventVenue: String? = null,
    val userId: Int? = null,
    val studentName: String? = null,
    val studentSurname: String? = null,
    val studentPatronymic: String? = null,
    val studentEmail: String? = null,
    val joinedAt: String? = null
)