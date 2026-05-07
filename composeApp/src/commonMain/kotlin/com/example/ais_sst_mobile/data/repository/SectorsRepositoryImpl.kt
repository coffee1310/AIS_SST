package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

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
        // TODO: POST запрос
    }

    override suspend fun leaveSector(id: Int): Result<Unit> = runCatching {
        // TODO: POST
    }
}