package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyAndCreateRequest(
    val email: String,
    val code: String,
    val accountRequest: RegisterRequest
)