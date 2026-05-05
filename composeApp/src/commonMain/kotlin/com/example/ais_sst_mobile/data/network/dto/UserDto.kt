package com.example.ais_sst_mobile.data.network.dto

import com.example.ais_sst_mobile.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: Int,
    val name: String,
    val surname: String,
    val patronymic: String? = null,
    val events_count: Int? = null,
    val points_count: Int? = null,
    val rank: Int? = null,
    val dateOfBirth: String,
    val courseNumber: Int,
    val specialityTitle: String,
    val groupTitle: String,
    val studentEmail: String,
    val additionalEmail: String? = null,
    val phoneNumber: String,
    val vkLink: String? = null,
    val photo: String? = null,
    val roleTitle: String,
    val gender: String? = null,
    val socialStatuses: List<String>? = null
) {
    fun toDomain(): User {
        return User(
            id = id,
            name = name,
            surname = surname,
            patronymic = patronymic,
            eventsCount = events_count,
            pointsCount = points_count,
            rank = rank,
            dateOfBirth = dateOfBirth,
            courseNumber = courseNumber,
            specialityTitle = specialityTitle,
            groupTitle = groupTitle,
            studentEmail = studentEmail,
            additionalEmail = additionalEmail,
            phoneNumber = phoneNumber,
            vkLink = vkLink,
            photo = photo,
            roleTitle = roleTitle,
            gender = gender,
            socialStatuses = socialStatuses
        )
    }
}