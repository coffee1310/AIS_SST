package com.example.ais_sst_mobile.core.network

import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.network.dto.AuthResponse
import com.example.ais_sst_mobile.data.network.dto.RefreshRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
                        ?: return@refreshTokens null

                    try {
                        val refreshClient = HttpClient {
                            install(ContentNegotiation) {
                                json(Json { ignoreUnknownKeys = true })
                            }
                        }
                        val response = refreshClient.post("${BASE_URL}auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(RefreshRequest(currentRefreshToken))
                        }.body<AuthResponse>()

                        sessionManager.saveAuthToken(response.token)
                        sessionManager.saveRefreshToken(response.refreshToken)

                        BearerTokens(response.token, response.refreshToken)
                    } catch (e: Exception) {
                        sessionManager.logout()
                        null
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