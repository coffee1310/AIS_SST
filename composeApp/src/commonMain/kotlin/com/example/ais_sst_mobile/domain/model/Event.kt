package com.example.ais_sst_mobile.domain.model

import com.example.ais_sst_mobile.data.network.dto.OrganizerDto

data class Event(
    val id: Int,
    val title: String,
    val description: String,
    val venue: String,
    val photoBase64: String?,
    val dateStrCard: String,
    val dateStrDetails: String,
    val rawDate: String,
    val isDraft: Boolean,
    val isCompleted: Boolean,
    val isOverdue: Boolean = false,
    val relationBadge: String? = null,
    val isPublic: Boolean = false,
    val organizers: List<OrganizerDto> = emptyList()
)