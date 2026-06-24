package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.domain.repository.PortfolioRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class PortfolioRepositoryImpl(
    private val httpClient: HttpClient
) : PortfolioRepository {

    override suspend fun uploadPortfolio(
        fileBytes: ByteArray,
        fileName: String
    ): Result<Unit> = runCatching {

        // Очищаем имя файла от русских символов и спецсимволов
        val safeFileName = sanitizeFileName(fileName)

        val response = httpClient.post("portfolio/upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = fileBytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
                                append(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.File
                                        .withParameter(ContentDisposition.Parameters.FileName, safeFileName)
                                        .toString()
                                )
                            }
                        )
                    }
                )
            )
        }

        if (!response.status.isSuccess()) {
            val errorBody = try {
                response.bodyAsText()
            } catch (e: Exception) {
                "Не удалось прочитать ответ сервера"
            }
            throw Exception("Ошибка загрузки портфолио (${response.status.value}): $errorBody")
        }
    }

    override suspend fun downloadPortfolio(): Result<ByteArray> = runCatching {
        val response = httpClient.get("portfolio/download")

        if (!response.status.isSuccess()) {
            val errorBody = try { response.bodyAsText() } catch (e: Exception) { "" }
            throw Exception("Ошибка скачивания портфолио (${response.status.value}): $errorBody")
        }

        response.body<ByteArray>()
    }

    /**
     * Очищает имя файла от недопустимых символов.
     * Заменяет русские буквы и спецсимволы на "_".
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")   // оставляем только латиницу, цифры, ., _ и -
            .replace(Regex("_{2,}"), "_")              // убираем повторяющиеся подчёркивания
            .take(120)                                 // ограничиваем длину
    }
}