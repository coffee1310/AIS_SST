package com.example.ais_sst_mobile.data.network.dto

import com.example.ais_sst_mobile.domain.model.SocialStatus
import com.example.ais_sst_mobile.domain.model.Speciality
import com.example.ais_sst_mobile.domain.model.Group
import kotlinx.serialization.Serializable

@Serializable
data class SpecialityDto(
    val id: Int,
    val title: String
)

@Serializable
data class SocialStatusDto(
    val id: Int,
    val title: String
)

@Serializable
data class GroupDto(
    val id: Int,
    val title: String,
    val course: Int
) {
    fun toDomain(): Group {
        return Group(
            id = id,
            title = title,
            course = course
        )
    }
}
@Serializable
data class SpecialitiesResponseWrapper(
    val data: List<SpecialityDto>
)

@Serializable
data class SocialStatusesResponseWrapper(
    val data: List<SocialStatusDto>
)

@Serializable
data class GroupsResponseWrapper(
    val data: List<GroupDto>
)

fun SpecialityDto.toDomain() = Speciality(id = id, title = title)
fun SocialStatusDto.toDomain() = SocialStatus(id = id, title = title)