package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

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
    val isDeleted: Boolean? = false,
    val organizers: List<OrganizerDto>? = emptyList(),
    val isFreeEvent: Boolean? = false,
    val maxParticipantsCount: Int? = 0,
    val maxOrganizersCount: Int? = 0,
    val currentParticipantsCount: Int? = 0,
    val currentOrganizersCount: Int? = 0,
    val sectorTitle: String? = null,
    val isMySector: Boolean? = null
)

@Serializable
data class PagedEventResponse(
    val content: List<EventDto>,
    val totalElements: Int,
    val totalPages: Int
)

@Serializable
data class OrganizerDto(
    val id: Int,
    val userId: Int,
    val userName: String,
    val userSurname: String,
    val userPatronymic: String? = null,
    val userEmail: String? = null,
    val userPhoto: String? = null
)

@Serializable
data class CreateEventRequestDto(
    val title: String,
    val description: String,
    val photo: String,
    val dateOfEvent: String,
    val startTime: String,
    val endTime: String,
    val venue: String,
    val organizerIds: List<Int>,
    val referenceToPosition: String,
    val isPublic: Boolean,
    val isDraft: Boolean,
    val isFreeEvent: Boolean,
    val maxParticipantsCount: Int?,
    val maxOrganizersCount: Int,
    val sectorIds: List<Int>
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
data class RoleDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val sectorId: Int,
    val sectorTitle: String? = null,
    val defaultPoints: Int? = null,
    val createdAt: String? = null, // ИСПРАВЛЕНИЕ: Добавили поля
    val updatedAt: String? = null  // ИСПРАВЛЕНИЕ: Добавили поля
)

@Serializable
data class EventRoleDto(
    val id: Int,
    val eventId: Int,
    val eventTitle: String? = null, // Сделали nullable
    val globalEventRoleId: Int,
    val globalEventRoleTitle: String,
    val capacity: Int,
    val reserveCapacity: Int,
    val description: String? = null, // Сделали nullable
    val deadline: String? = null,    // Сделали nullable
    val totalOccupiedSlots: Int? = 0,
    val totalAvailableSlots: Int? = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class PagedEventRoleResponse(
    val content: List<EventRoleDto>
)