package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.CreateRoleRequestDto
import com.example.ais_sst_mobile.data.network.dto.GroupDto
import com.example.ais_sst_mobile.data.network.dto.SocialStatusDto
import com.example.ais_sst_mobile.data.network.dto.SpecialityDto
import com.example.ais_sst_mobile.data.network.dto.toDomain
import com.example.ais_sst_mobile.domain.model.SocialStatus
import com.example.ais_sst_mobile.domain.model.Group
import com.example.ais_sst_mobile.domain.model.Speciality
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import com.example.ais_sst_mobile.data.network.dto.EventGlobalRoleDto
import com.example.ais_sst_mobile.data.network.dto.GroupsResponseWrapper
import com.example.ais_sst_mobile.data.network.dto.SocialStatusesResponseWrapper
import com.example.ais_sst_mobile.data.network.dto.SpecialitiesResponseWrapper
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class DictionaryRepositoryImpl(
    private val httpClient: HttpClient
) : DictionaryRepository {

    override suspend fun getSpecialities(): Result<List<Speciality>> = runCatching {
        val response = httpClient.get("specialities").body<SpecialitiesResponseWrapper>()
        response.data.map { it.toDomain() }
    }

    override suspend fun getSocialStatuses(): Result<List<SocialStatus>> = runCatching {
        val response = httpClient.get("social_status").body<SocialStatusesResponseWrapper>()
        response.data.map { it.toDomain() }
    }

    override suspend fun getGroups(): Result<List<Group>> = runCatching {
        val response = httpClient.get("group").body<GroupsResponseWrapper>()
        response.data.map { it.toDomain() }
    }

    override suspend fun getEventRoles(): Result<List<EventGlobalRoleDto>> = runCatching {
        httpClient.get("roles").body()
    }

    override suspend fun getEventRoleById(id: Int): Result<EventGlobalRoleDto> = runCatching {
        httpClient.get("roles/$id").body<EventGlobalRoleDto>()
    }

    override suspend fun createEventRole(request: CreateRoleRequestDto): Result<EventGlobalRoleDto> = runCatching {
        val response = httpClient.post("roles") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            response.body<EventGlobalRoleDto>()
        } else {
            throw Exception("Ошибка сервера (${response.status.value}). Роль не создана.")
        }
    }

    override suspend fun updateEventRole(id: Int, request: CreateRoleRequestDto): Result<EventGlobalRoleDto> = runCatching {
        val response = httpClient.put("roles/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) {
            response.body<EventGlobalRoleDto>()
        } else {
            throw Exception("Ошибка сервера (${response.status.value}). Роль не обновлена.")
        }
    }
}