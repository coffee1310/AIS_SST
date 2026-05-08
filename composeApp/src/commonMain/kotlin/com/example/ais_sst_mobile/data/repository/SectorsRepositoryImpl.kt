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
import io.ktor.client.request.put


class SectorsRepositoryImpl(
    private val httpClient: HttpClient
) : SectorsRepository {

    override suspend fun getSectors(): Result<List<SectorDto>> = runCatching {
        httpClient.get("sector").body<List<SectorDto>>()
    }
    override suspend fun getSectorById(id: Int): Result<SectorDto> = runCatching {
        val sectors = httpClient.get("sector").body<List<SectorDto>>()
        sectors.first { it.id == id }
    }

    override suspend fun joinSector(id: Int): Result<Unit> = runCatching {
        val response = httpClient.post("sector/$id")

        if (!response.status.isSuccess()) {
            throw Exception("Не удалось отправить заявку")
        }
    }

    override suspend fun leaveSector(id: Int): Result<Unit> = runCatching {
        // TODO: POST
    }
    override suspend fun getSectorParticipants(sectorId: Int, page: Int): Result<ParticipantResponseDto> = runCatching {
        httpClient.get("sector/$sectorId/participants") {
            parameter("page", page)
            parameter("size", 20)
            parameter("sortBy", "entryDate")
            parameter("sortDirection", "DESC")
        }.body()
    }
    override suspend fun getSectorRequests(): Result<List<SectorRequestDto>> = runCatching {
        httpClient.get("sector/introductions").body()
    }
    override suspend fun acceptSectorRequest(requestId: Int): Result<String> = runCatching {
        val response: SectorRequestActionResponseDto = httpClient.put("sector/accept/$requestId").body()
        response.message
    }

    override suspend fun rejectSectorRequest(requestId: Int): Result<String> = runCatching {
        val response: SectorRequestActionResponseDto = httpClient.put("sector/reject/$requestId").body()
        response.message
    }
}