package com.example.ais_sst_mobile.domain.repository

import com.example.ais_sst_mobile.data.network.dto.CreateSectorRequestDto
import com.example.ais_sst_mobile.data.network.dto.ParticipantResponseDto
import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.data.network.dto.SectorRequestDto

interface SectorsRepository {
    suspend fun getSectors(): Result<List<SectorDto>>
    suspend fun getSectorById(id: Int): Result<SectorDto>
    suspend fun joinSector(id: Int): Result<Unit>
    suspend fun leaveSector(id: Int): Result<Unit>
    suspend fun getSectorParticipants(sectorId: Int, page: Int = 0): Result<ParticipantResponseDto>
    suspend fun getSectorRequests(sectorId: Int? = null): Result<List<SectorRequestDto>>
    suspend fun acceptSectorRequest(requestId: Int): Result<String>
    suspend fun rejectSectorRequest(requestId: Int): Result<String>
    suspend fun kickParticipant(sectorId: Int, userId: Int): Result<Unit>
    suspend fun createSector(request: CreateSectorRequestDto): Result<Unit>
}