package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateSectorRequestDto(
    val title: String,
    val description: String,
    val coordinatorIds: List<Int>,
    val photo: String?
)