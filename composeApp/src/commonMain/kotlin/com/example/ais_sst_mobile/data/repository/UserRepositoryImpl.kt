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
import com.example.ais_sst_mobile.data.network.dto.UserPageResponseDto


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
    override suspend fun getActivists(page: Int, size: Int, searchQuery: String): Result<UserPageResponseDto> = runCatching {
        httpClient.get("users/all") {
            parameter("page", page)
            parameter("size", size)
            parameter("sortBy", "surname")
            parameter("sortDirection", "ASC")
            parameter("role", "Activist")
            if (searchQuery.isNotBlank()) {
                parameter("search", searchQuery)
            }
        }.body()
    }
    override suspend fun getAllUsers(page: Int, size: Int, searchQuery: String): Result<UserPageResponseDto> = runCatching {
        httpClient.get("http://185.246.66.164:8080/api/users/all") {
            parameter("page", page)
            parameter("size", size)
            parameter("sortBy", "surname")
            parameter("sortDirection", "ASC")
            if (searchQuery.isNotBlank()) {
                parameter("search", searchQuery)
            }
        }.body()
    }
    override suspend fun getUsersByRole(role: String): Result<List<UserProfileDto>> = runCatching {
        val response = httpClient.get("users/all") {
            parameter("page", 0)
            parameter("size", 50)
            parameter("sortBy", "id")
            parameter("sortDirection", "ASC")
            parameter("role", role)
        }.body<PageableResponse<UserProfileDto>>()
        response.content
    }
}