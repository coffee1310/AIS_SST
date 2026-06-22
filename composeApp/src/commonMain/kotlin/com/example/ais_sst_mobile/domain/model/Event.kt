package com.example.ais_sst_mobile.domain.model

data class Organizer(
    val userId: Int,
    val userName: String,
    val userSurname: String,
    val userPhoto: String?,
    val groupInfo: String = "Организатор мероприятия"
)

data class Event(
    val id: Int,
    val title: String,
    val description: String,
    val venue: String,
    val photoBase64: String?,
    val dateStrCard: String,
    val dateStrDetails: String,
    val rawDate: String,
    val eventCreatorId: Int,
    val isDraft: Boolean,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val isPublic: Boolean = false,
    val organizers: List<Organizer> = emptyList(),
    val relationBadge: String? = null,
    val isFreeEvent: Boolean = false,
    val maxParticipantsCount: Int = 0,
    val maxOrganizersCount: Int = 0,
    val currentParticipantsCount: Int = 0,
    val currentOrganizersCount: Int = 0,
    val sectorTitle: String? = null,
    val isMySector: Boolean = false
)