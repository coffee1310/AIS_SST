package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import com.example.ais_sst_mobile.domain.model.User
import com.example.ais_sst_mobile.domain.repository.UserRepository
import com.example.ais_sst_mobile.data.network.dto.AccountRequestDto
import com.example.ais_sst_mobile.data.network.dto.PageableResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class UserRepositoryImpl(
    private val httpClient: HttpClient
) : UserRepository {

    override suspend fun getUserProfile(): Result<User> = runCatching {
        val response: UserProfileDto = httpClient.get("users/me").body()
        response.toDomain()
    }
    override suspend fun getAccountRequests(): Result<List<AccountRequestDto>> = runCatching {
        val response = httpClient.get("account_requests").body<PageableResponse<AccountRequestDto>>()
        response.content
    }
}