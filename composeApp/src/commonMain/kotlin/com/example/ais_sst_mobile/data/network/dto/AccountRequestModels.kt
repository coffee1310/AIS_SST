package com.example.ais_sst_mobile.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PageableResponse<T>(
    val content: List<T>,
    val totalElements: Int,
    val totalPages: Int
)

@Serializable
data class AccountRequestDto(
    val id: Int,
    val name: String,
    val surname: String,
    val patronymic: String?,
    val gender: String?,
    val dateOfBirth: String?,
    val studentEmail: String?,
    val additionalEmail: String? = null,
    val phoneNumber: String?,
    val studentIdNumber: Int?,
    val courseNumber: Int?,
    val status: String?,
    val reasonForRefusal: String? = null,
    val groupId: Int?,
    val groupName: String? = null,
    val specialityId: Int?,
    val specialityName: String? = null,
    val photo: String? = null,
    val vkLink: String? = null,
    val socialStatuses: List<String>? = null,
    val shortSpecialityTitle: String? = null
)
@Serializable
data class RejectRequestDto(
    val rejectionReason: String
)