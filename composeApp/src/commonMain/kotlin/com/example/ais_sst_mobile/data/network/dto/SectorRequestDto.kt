package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SectorRequestDto(
    val id: Int,
    val sector_id: Int,
    val user_id: Int,
    val status: String,
    val name: String? = null,
    val surname: String? = null,
    val patronymic: String? = null,
    val photo: String? = null,
    val courseNumber: Int? = null,
    val specialityName: String? = null,
    val groupName: String? = null
)