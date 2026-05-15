package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import com.example.ais_sst_mobile.data.network.dto.ParticipantResponseDto
import com.example.ais_sst_mobile.data.network.dto.SectorRequestActionResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import com.example.ais_sst_mobile.data.network.dto.SectorRequestDto
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.example.ais_sst_mobile.data.network.dto.CreateSectorRequestDto
import com.example.ais_sst_mobile.data.network.dto.UpdateSectorRequestDto
import io.ktor.client.statement.bodyAsText


class SectorsRepositoryImpl(
    private val httpClient: HttpClient
) : SectorsRepository {

    override suspend fun getSectors(): Result<List<SectorDto>> = runCatching {
        httpClient.get("sector").body<List<SectorDto>>()
    }
    override suspend fun getSectorById(id: Int): Result<SectorDto> = runCatching {
        val details = httpClient.get("sector/$id").body<SectorDto>()
        val allSectors = httpClient.get("sector").body<List<SectorDto>>()
        val userStatus = allSectors.firstOrNull { it.id == id }

        if (userStatus != null) {
            details.copy(
                isParticipant = userStatus.isParticipant,
                isCoordinator = userStatus.isCoordinator,
                hasActiveRequest = userStatus.hasActiveRequest,
                requestStatus = userStatus.requestStatus,
                participantCount = userStatus.participantCount
            )
        } else {
            details
        }
    }

    override suspend fun joinSector(id: Int): Result<Unit> = runCatching {
        val response = httpClient.post("sector/$id")

        if (!response.status.isSuccess()) {
            throw Exception("Не удалось отправить заявку")
        }
    }
    override suspend fun leaveSector(id: Int): Result<Unit> = runCatching {
        val response = httpClient.delete("sector/$id/leave")
        if (!response.status.isSuccess()) {
            throw Exception("Ошибка при выходе из сектора: ${response.status.value}")
        }
    }
    override suspend fun getSectorParticipants(sectorId: Int, page: Int): Result<ParticipantResponseDto> = runCatching {
        httpClient.get("sector/$sectorId/participants") {
            parameter("page", page)
            parameter("size", 20)
            parameter("sortBy", "entryDate")
            parameter("sortDirection", "DESC")
        }.body()
    }
    override suspend fun getSectorRequests(sectorId: Int? ): Result<List<SectorRequestDto>> = runCatching {
        httpClient.get("sector/introductions/filter") {
            parameter("status", "НА_РАССМОТРЕНИИ")
            if (sectorId != null) {
                parameter("sectorId", sectorId)
            }
        }.body()
    }
    override suspend fun acceptSectorRequest(requestId: Int): Result<String> = runCatching {
        val response: SectorRequestActionResponseDto = httpClient.put("sector/accept/$requestId").body()
        response.message
    }

    override suspend fun rejectSectorRequest(requestId: Int): Result<String> = runCatching {
        val response: SectorRequestActionResponseDto = httpClient.put("sector/reject/$requestId").body()
        response.message
    }
    override suspend fun kickParticipant(sectorId: Int, userId: Int): Result<Unit> = runCatching {
        val response = httpClient.delete("sector/$sectorId/kick/$userId")
        if (!response.status.isSuccess()) {
            throw Exception("Ошибка при исключении активиста: ${response.status.value}")
        }
    }
    override suspend fun createSector(request: CreateSectorRequestDto): Result<Unit> = runCatching {
        httpClient.post("sector") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    override suspend fun removeCoordinator(sectorId: Int, userId: Int): Result<Unit> = runCatching {
        val response = httpClient.delete("sector/$sectorId/coordinator/$userId")
        if (!response.status.isSuccess()) {
            throw Exception("Ошибка при удалении координатора: ${response.status.value}")
        }
    }
    override suspend fun appointCoordinator(sectorId: Int, userId: Int): Result<Unit> = runCatching {
        val response = httpClient.post("sector/appoint_coordinator/$sectorId") {
            parameter("user_id", userId)
        }
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val errorMessage = if (errorBody.contains("\"message\"")) {
                errorBody.substringAfter("\"message\":\"").substringBefore("\"")
            } else {
                "Ошибка при назначении координатора: ${response.status.value}"
            }
            throw Exception(errorMessage)
        }
    }
    override suspend fun updateSector(id: Int, request: UpdateSectorRequestDto): Result<Unit> = runCatching {
        val response = httpClient.put("sector/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val errorMessage = if (errorBody.contains("\"message\"")) {
                errorBody.substringAfter("\"message\":\"").substringBefore("\"")
            } else {
                "Ошибка при обновлении сектора: ${response.status.value}"
            }
            throw Exception(errorMessage)
        }
    }
}