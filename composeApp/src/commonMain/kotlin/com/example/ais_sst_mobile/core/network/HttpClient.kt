package com.example.ais_sst_mobile.core.network

import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.network.dto.AuthResponse
import com.example.ais_sst_mobile.data.network.dto.RefreshRequest
import com.example.ais_sst_mobile.data.network.dto.RefreshResponseWrapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal const val BASE_URL = "http://185.246.66.164:8080/api/"

fun createHttpClient(sessionManager: SessionManager): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("Ktor: $message")
                }
            }
            level = LogLevel.ALL
        }
        // мб удалить, если будут случайные вылеты
        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                if (statusCode == 401 || statusCode == 403) {
                    sessionManager.logout()
                }
            }
        }
        defaultRequest {
            url(BASE_URL)
            contentType(ContentType.Application.Json)
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val accessToken = sessionManager.fetchAuthToken()
                    val refreshToken = sessionManager.fetchRefreshToken()
                    if (accessToken != null && refreshToken != null) {
                        BearerTokens(accessToken, refreshToken)
                    } else null
                }

                refreshTokens {
                    val currentRefreshToken = sessionManager.fetchRefreshToken()
                    if (currentRefreshToken == null) return@refreshTokens null

                    try {
                        val refreshClient = HttpClient {
                            install(ContentNegotiation) {
                                json(Json { ignoreUnknownKeys = true })
                            }
                        }

                        val wrapper: RefreshResponseWrapper = refreshClient.post("${BASE_URL}auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody<RefreshRequest>(RefreshRequest(currentRefreshToken))
                        }.body<RefreshResponseWrapper>()

                        val response = wrapper.data

                        sessionManager.saveAuthToken(response.token)
                        sessionManager.saveRefreshToken(response.refreshToken)

                        return@refreshTokens BearerTokens(response.token, response.refreshToken)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        sessionManager.logout()
                        return@refreshTokens null
                    }
                }
                sendWithoutRequest { request ->
                    val path = request.url.encodedPath
                    !path.contains("auth/login") && !path.contains("auth/refresh")
                }
            }
        }
    }
}