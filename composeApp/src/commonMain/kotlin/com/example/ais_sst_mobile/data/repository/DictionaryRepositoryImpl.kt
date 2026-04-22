package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.SocialStatusDto
import com.example.ais_sst_mobile.data.network.dto.SpecialityDto
import com.example.ais_sst_mobile.data.network.dto.toDomain
import com.example.ais_sst_mobile.domain.model.SocialStatus
import com.example.ais_sst_mobile.domain.model.Speciality
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class DictionaryRepositoryImpl(
    private val httpClient: HttpClient
) : DictionaryRepository {

    override suspend fun getSpecialities(): Result<List<Speciality>> = runCatching {
        val response: List<SpecialityDto> = httpClient.get("specialities").body()
        response.map { it.toDomain() }
    }

    override suspend fun getSocialStatuses(): Result<List<SocialStatus>> = runCatching {
        val response: List<SocialStatusDto> = httpClient.get("social_status").body()
        response.map { it.toDomain() }
    }
}