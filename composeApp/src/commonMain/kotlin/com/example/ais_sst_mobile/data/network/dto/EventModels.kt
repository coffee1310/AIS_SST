package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PagedEventResponse(
    val content: List<EventDto>,
    val totalElements: Int,
    val totalPages: Int
)

@Serializable
data class EventDto(
    val id: Int,
    val title: String,
    val description: String?,
    val photo: String?,
    val dateOfEvent: String,
    val startTime: String,
    val endTime: String,
    val venue: String,
    val eventCreatorId: Int,
    val eventCreatorName: String?,
    val eventCreatorSurname: String?,
    val isPublic: Boolean,
    val isDraft: Boolean,
    val isActive: Boolean,
    val isCompleted: Boolean,
    val organizers: List<OrganizerDto>? = emptyList()
)
@Serializable
data class RoleDto(
    val id: Int,
    val title: String,
    val description: String?,
    val sectorId: Int?,
    val sectorTitle: String?
)

@Serializable
data class CreateEventRequestDto(
    val title: String,
    val description: String,
    val photo: String?,
    val dateOfEvent: String,
    val startTime: String,
    val endTime: String,
    val venue: String,
    val organizerIds: List<Int>,
    val referenceToPosition: String,
    val isPublic: Boolean,
    val isDraft: Boolean
)

@Serializable
data class CreateEventRoleRequestDto(
    val eventId: Int,
    val globalEventRoleId: Int,
    val capacity: Int,
    val reserveCapacity: Int,
    val deadline: String,
    val description: String
)

@Serializable
data class EventRoleDto(
    val id: Int,
    val eventId: Int,
    val eventTitle: String,
    val globalEventRoleId: Int,
    val globalEventRoleTitle: String,
    val capacity: Int,
    val reserveCapacity: Int,
    val description: String,
    val deadline: String
)

@Serializable
data class PagedEventRoleResponse(
    val content: List<EventRoleDto>
)

@Serializable
data class OrganizerDto(
    val id: Int,
    val userId: Int,
    val userName: String,
    val userSurname: String
)