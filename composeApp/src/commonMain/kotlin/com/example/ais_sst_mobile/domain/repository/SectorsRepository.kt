package com.example.ais_sst_mobile.domain.repository

import com.example.ais_sst_mobile.data.network.dto.SectorDto

interface SectorsRepository {
    suspend fun getSectors(): Result<List<SectorDto>>
    suspend fun getSectorById(id: Int): Result<SectorDto>
    suspend fun joinSector(id: Int): Result<Unit>
    suspend fun leaveSector(id: Int): Result<Unit>
}