package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventGlobalRoleDto(
    val id: Int,
    val title: String,
    val description: String,
    val sectorId: Int,
    val sectorTitle: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
@Serializable
data class CreateRoleRequestDto(
    val title: String,
    val description: String,
    @SerialName("sector_id") val sectorId: Int,
    val isDefaultRole: Boolean = true
)