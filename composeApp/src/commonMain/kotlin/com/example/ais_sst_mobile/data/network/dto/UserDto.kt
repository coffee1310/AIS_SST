package com.example.ais_sst_mobile.data.network.dto

import com.example.ais_sst_mobile.domain.model.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserProfileDto(
    val id: Int,
    val name: String,
    val surname: String,
    val patronymic: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val studentEmail: String? = null,
    val additionalEmail: String? = null,
    val phoneNumber: String? = null,
    val vkLink: String? = null,
    val photo: String? = null,
    val courseNumber: Int? = null,
    val role: String? = null,
    val isActive: Boolean? = null,
    val isBanned: Boolean? = null,
    val studentIdNumber: Int? = null,
    val groupName: String? = null,
    val specialityName: String? = null,
    val groupId: Int? = null,
    val specialityId: Int? = null,
    @SerialName("events_count")
    val eventsCount: Int? = null,
    @SerialName("points_count")
    val pointsCount: Int? = null,
    val rank: Int? = null,
    val roleTitle: String? = null,
    val coordinatorSector: String? = null,
    val socialStatuses: List<String>? = null,
    val coordinatorSectorId: Int? = null,
    val coordinatorSectorTitle: String? = null,
    val shortSpecialityTitle: String? = null,
    val specialityShortTitle: String? = null,
    val groupTitle: String? = null,
    val specialityTitle: String? = null
) {
    fun toDomain(): User {
        return User(
            id = id,
            name = name,
            surname = surname,
            patronymic = patronymic,
            eventsCount = eventsCount ?: 0,
            pointsCount = pointsCount ?: 0,
            rank = rank,
            dateOfBirth = dateOfBirth ?: "",
            courseNumber = courseNumber ?: 0,
            specialityTitle = specialityTitle ?: specialityName ?: "",
            shortSpecialityTitle = shortSpecialityTitle ?: specialityShortTitle ?: "",
            groupTitle = groupTitle ?: groupName ?: "",
            studentEmail = studentEmail ?: "",
            additionalEmail = additionalEmail,
            phoneNumber = phoneNumber ?: "",
            vkLink = vkLink,
            photo = photo,
            roleTitle = roleTitle ?: role ?: "",
            gender = gender,
            socialStatuses = socialStatuses,
            coordinatorSectorId = this.coordinatorSectorId,
            coordinatorSectorTitle = this.coordinatorSectorTitle ?: this.coordinatorSector
        )
    }
}