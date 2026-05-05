package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.AuthResponse
import com.example.ais_sst_mobile.data.network.dto.LoginRequest
import com.example.ais_sst_mobile.data.network.dto.RegisterRequest
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.plugin
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AuthRepositoryImpl(
    private val httpClient: HttpClient
) : AuthRepository {

    override suspend fun login(request: LoginRequest): Result<AuthResponse> = runCatching {
        httpClient.plugin(Auth).providers.filterIsInstance<BearerAuthProvider>().firstOrNull()?.clearToken()

        val response = httpClient.post("auth/login") {
            setBody(request)
        }.body<AuthResponse>()

        response
    }

    override suspend fun register(request: RegisterRequest): Result<Unit> = runCatching {
        val response = httpClient.post("account_requests") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()

            if (response.status.value == 500) {
                throw Exception("Пользователь с такой почтой или телефоном уже существует")
            } else {
                throw Exception("Ошибка регистрации (${response.status.value}). Попробуйте позже.")

            }
        }
    }
}