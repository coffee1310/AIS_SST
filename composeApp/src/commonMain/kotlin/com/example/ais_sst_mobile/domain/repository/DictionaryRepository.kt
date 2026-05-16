package com.example.ais_sst_mobile.domain.repository

import com.example.ais_sst_mobile.data.network.dto.CreateRoleRequestDto
import com.example.ais_sst_mobile.data.network.dto.EventGlobalRoleDto
import com.example.ais_sst_mobile.domain.model.Group
import com.example.ais_sst_mobile.domain.model.SocialStatus
import com.example.ais_sst_mobile.domain.model.Speciality

interface DictionaryRepository {
    suspend fun getSpecialities(): Result<List<Speciality>>
    suspend fun getSocialStatuses(): Result<List<SocialStatus>>
    suspend fun getGroups(): Result<List<Group>>
    suspend fun getEventRoles(): Result<List<EventGlobalRoleDto>>
    suspend fun getEventRoleById(id: Int): Result<EventGlobalRoleDto>
    suspend fun createEventRole(request: CreateRoleRequestDto): Result<EventGlobalRoleDto>
    suspend fun updateEventRole(id: Int, request: CreateRoleRequestDto): Result<EventGlobalRoleDto>
}