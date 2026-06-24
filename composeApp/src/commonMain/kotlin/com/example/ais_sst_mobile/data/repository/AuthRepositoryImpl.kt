package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.AuthResponse
import com.example.ais_sst_mobile.data.network.dto.LoginRequest
import com.example.ais_sst_mobile.data.network.dto.LoginResponseWrapper
import com.example.ais_sst_mobile.data.network.dto.RegisterRequest
import com.example.ais_sst_mobile.data.network.dto.VerifyAndCreateRequest
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
        val wrapper = httpClient.post("auth/login") {
            setBody(request)
        }.body<LoginResponseWrapper>()
        wrapper.data
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
    override suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        val response = httpClient.post("auth/password-reset/request") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to email))
        }

        if (!response.status.isSuccess()) {
            throw Exception("Не удалось отправить код. Проверьте email.")
        }
    }

    override suspend fun verifyPasswordReset(
        email: String,
        code: String,
        newPassword: String
    ): Result<Unit> = runCatching {
        val response = httpClient.post("auth/password-reset/verify") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "email" to email,
                    "code" to code,
                    "newPassword" to newPassword
                )
            )
        }

        if (!response.status.isSuccess()) {
            val errorText = try {
                response.bodyAsText()
            } catch (e: Exception) {
                ""
            }
            throw Exception("Неверный код или ошибка сброса пароля. $errorText")
        }
    }

    override suspend fun sendRegistrationCode(
        name: String,
        surname: String,
        studentEmail: String
    ): Result<Unit> = runCatching {
        httpClient.post("account_requests/send-code") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "name" to name,
                "surname" to surname,
                "studentEmail" to studentEmail
            ))
        }.also {
            if (!it.status.isSuccess()) throw Exception("Не удалось отправить код")
        }
    }

    override suspend fun verifyAndCreateAccount(
        email: String,
        code: String,
        accountRequest: RegisterRequest
    ): Result<Unit> = runCatching {
        val request = VerifyAndCreateRequest(
            email = email,
            code = code,
            accountRequest = accountRequest
        )

        val response = httpClient.post("account_requests/verify-and-create") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("Ошибка подтверждения кода: $errorBody")
        }
    }
}