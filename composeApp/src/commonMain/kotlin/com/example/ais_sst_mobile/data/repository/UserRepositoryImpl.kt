package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import com.example.ais_sst_mobile.domain.model.User
import com.example.ais_sst_mobile.domain.repository.UserRepository
import com.example.ais_sst_mobile.data.network.dto.AccountRequestDto
import com.example.ais_sst_mobile.data.network.dto.PageableResponse
import com.example.ais_sst_mobile.data.network.dto.RejectRequestDto
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class UserRepositoryImpl(
    private val httpClient: HttpClient
) : UserRepository {

    override suspend fun getUserProfile(): Result<User> = runCatching {
        val response: UserProfileDto = httpClient.get("users/me").body()
        response.toDomain()
    }
    override suspend fun getAccountRequests(): Result<List<AccountRequestDto>> = runCatching {
        val response = httpClient.get("account_requests/pending") {
            parameter("page", 0)
            parameter("size", 20)
        }.body<PageableResponse<AccountRequestDto>>()

        response.content
    }
    override suspend fun rejectAccountRequest(id: Int, reason: String): Result<Unit> = runCatching {
        httpClient.put("account_requests/reject/$id") {
            contentType(ContentType.Application.Json)
            setBody(RejectRequestDto(rejectionReason = reason))
        }
    }
    override suspend fun acceptAccountRequest(id: Int): Result<Unit> = runCatching {
        httpClient.put("account_requests/accept/$id")
    }
    override suspend fun getAccountRequestById(id: Int): Result<AccountRequestDto> = runCatching {
        val response = httpClient.get("account_requests/filter") {
            parameter("id", id)
        }.body<PageableResponse<AccountRequestDto>>()

        response.content.firstOrNull() ?: throw Exception("Заявка не найдена")
    }
    override suspend fun getUserProfileById(id: Int): Result<UserProfileDto> = runCatching {
        val response = httpClient.get("users/all") {
            parameter("id", id)
        }.body<PageableResponse<UserProfileDto>>()
        response.content.firstOrNull() ?: throw Exception("Пользователь не найден")
    }
}