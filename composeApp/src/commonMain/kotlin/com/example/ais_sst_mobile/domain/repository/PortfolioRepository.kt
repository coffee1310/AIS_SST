package com.example.ais_sst_mobile.domain.repository

interface PortfolioRepository {
    suspend fun uploadPortfolio(fileBytes: ByteArray, fileName: String): Result<Unit>
    suspend fun downloadPortfolio(): Result<ByteArray>
}