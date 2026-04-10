package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.AuthResponse
import com.example.ais_sst_mobile.data.network.dto.LoginRequest
import com.example.ais_sst_mobile.data.network.dto.RegisterRequest
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val settings: Settings
) : AuthRepository {

    override suspend fun login(request: LoginRequest): Result<AuthResponse> = runCatching {
        val response = httpClient.post("auth/login") {
            setBody(request)
        }.body<AuthResponse>()

        settings.putString("jwt_token", response.token)
        settings.putString("user_role", response.roles.joinToString(","))
        response
    }

    override suspend fun register(request: RegisterRequest): Result<Unit> = runCatching {
        httpClient.post("auth/register") {
            setBody(request)
        }
    }
}