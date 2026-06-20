package com.example.ais_sst_mobile.data.repository

import com.example.ais_sst_mobile.data.network.dto.CreateEventRequestDto
import com.example.ais_sst_mobile.data.network.dto.CreateEventRoleRequestDto
import com.example.ais_sst_mobile.data.network.dto.EventDto
import com.example.ais_sst_mobile.data.network.dto.PagedEventResponse
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import com.example.ais_sst_mobile.data.network.dto.RoleDto
import com.example.ais_sst_mobile.data.network.dto.EventRoleDto
import com.example.ais_sst_mobile.data.network.dto.PagedEventRoleResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class EventsRepositoryImpl(
    private val httpClient: HttpClient
) : EventsRepository {

    private var hasDeletedEventSignal = false

    override fun getAndClearDeletedEventSignal(): Boolean {
        val current = hasDeletedEventSignal
        hasDeletedEventSignal = false
        return current
    }

    override suspend fun getUpcomingEvents(dateFrom: String, dateTo: String): Result<List<Event>> = runCatching {
        val response = httpClient.get("events") {
            parameter("dateFrom", dateFrom)
            parameter("dateTo", dateTo)
            parameter("size", 150)
            parameter("isDraft", false)
            parameter("isPublic", true)
        }.body<PagedEventResponse>()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        // Отфильтровываем удаленные и рассчитываем isOverdue
        response.content.filter { it.isDeleted != true }.map { dto ->
            Event(
                id = dto.id,
                title = dto.title,
                description = dto.description ?: "",
                venue = dto.venue,
                photoBase64 = dto.photo,
                dateStrCard = formatEventDate(dto.dateOfEvent, dto.startTime, null),
                dateStrDetails = formatEventDate(dto.dateOfEvent, dto.startTime, dto.endTime),
                rawDate = dto.dateOfEvent,
                eventCreatorId = dto.eventCreatorId,
                isDraft = dto.isDraft,
                isCompleted = dto.isCompleted,
                isOverdue = dto.dateOfEvent < today && !dto.isCompleted,
                isPublic = dto.isPublic,
                organizers = dto.organizers?.map { orgDto ->
                    com.example.ais_sst_mobile.domain.model.Organizer(
                        userId = orgDto.userId,
                        userName = orgDto.userName,
                        userSurname = orgDto.userSurname,
                        userPhoto = orgDto.userPhoto
                    )
                } ?: emptyList(),
                isFreeEvent = dto.isFreeEvent ?: false,
                maxParticipantsCount = dto.maxParticipantsCount ?: 0,
                maxOrganizersCount = dto.maxOrganizersCount ?: 0,
                currentParticipantsCount = dto.currentParticipantsCount ?: 0,
                currentOrganizersCount = dto.currentOrganizersCount ?: 0
            )
        }.sortedBy { it.rawDate }
    }

    private fun formatEventDate(date: String, start: String, end: String?): String {
        try {
            val parts = date.split("-")
            if (parts.size != 3) return date

            val day = parts[2].toInt().toString()
            val month = when(parts[1]) {
                "01" -> "января"; "02" -> "февраля"; "03" -> "марта"
                "04" -> "апреля"; "05" -> "мая"; "06" -> "июня"
                "07" -> "июля"; "08" -> "августа"; "09" -> "сентября"
                "10" -> "октября"; "11" -> "ноября"; "12" -> "декабря"
                else -> ""
            }

            val startTime = start.take(5)
            return if (end != null) {
                val endTime = end.take(5)
                "$day $month, $startTime - $endTime"
            } else {
                "$day $month, $startTime"
            }
        } catch (e: Exception) {
            return date
        }
    }

    override suspend fun getEventRoles(eventId: Int): Result<List<EventRoleDto>> = runCatching {
        httpClient.get("event-roles") {
            parameter("eventId", eventId)
        }.body<PagedEventRoleResponse>().content
    }

    override suspend fun getEventById(id: Int): Result<Event> = runCatching {
        val dto = httpClient.get("events/$id").body<EventDto>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        Event(
            id = dto.id,
            title = dto.title,
            description = dto.description ?: "",
            venue = dto.venue,
            photoBase64 = dto.photo,
            dateStrCard = formatEventDate(dto.dateOfEvent, dto.startTime, null),
            dateStrDetails = formatEventDate(dto.dateOfEvent, dto.startTime, dto.endTime),
            rawDate = dto.dateOfEvent,
            eventCreatorId = dto.eventCreatorId,
            isDraft = dto.isDraft,
            isCompleted = dto.isCompleted,
            isOverdue = dto.dateOfEvent < today && !dto.isCompleted, // ИСПРАВЛЕНО
            isPublic = dto.isPublic,
            organizers = dto.organizers?.map { orgDto ->
                com.example.ais_sst_mobile.domain.model.Organizer(
                    userId = orgDto.userId,
                    userName = orgDto.userName,
                    userSurname = orgDto.userSurname,
                    userPhoto = orgDto.userPhoto
                )
            } ?: emptyList(),
            isFreeEvent = dto.isFreeEvent ?: false,
            maxParticipantsCount = dto.maxParticipantsCount ?: 0,
            maxOrganizersCount = dto.maxOrganizersCount ?: 0,
            currentParticipantsCount = dto.currentParticipantsCount ?: 0,
            currentOrganizersCount = dto.currentOrganizersCount ?: 0
        )
    }

    override suspend fun getAvailableEvents(): Result<List<Event>> = runCatching {
        coroutineScope {
            val publicDeferred = async {
                httpClient.get("events") {
                    parameter("isPublic", true)
                    parameter("isDraft", false)
                    parameter("size", 150)
                }.body<PagedEventResponse>().content
            }

            val sectorDeferred = async {
                httpClient.get("events") {
                    parameter("isResponsibleSector", true)
                    parameter("isDraft", false)
                    parameter("size", 150)
                }.body<PagedEventResponse>().content
            }

            val allDto = publicDeferred.await() + sectorDeferred.await()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

            // Отфильтровываем удаленные и рассчитываем isOverdue
            allDto.filter { it.isDeleted != true }.distinctBy { it.id }.map { dto ->
                Event(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description ?: "",
                    venue = dto.venue,
                    photoBase64 = dto.photo,
                    dateStrCard = formatEventDate(dto.dateOfEvent, dto.startTime, null),
                    dateStrDetails = formatEventDate(dto.dateOfEvent, dto.startTime, dto.endTime),
                    rawDate = dto.dateOfEvent,
                    eventCreatorId = dto.eventCreatorId,
                    isDraft = dto.isDraft,
                    isCompleted = dto.isCompleted,
                    isOverdue = dto.dateOfEvent < today && !dto.isCompleted,
                    isPublic = dto.isPublic,
                    organizers = dto.organizers?.map { orgDto ->
                        com.example.ais_sst_mobile.domain.model.Organizer(
                            userId = orgDto.userId,
                            userName = orgDto.userName,
                            userSurname = orgDto.userSurname,
                            userPhoto = orgDto.userPhoto
                        )
                    } ?: emptyList(),
                    isFreeEvent = dto.isFreeEvent ?: false,
                    maxParticipantsCount = dto.maxParticipantsCount ?: 0,
                    maxOrganizersCount = dto.maxOrganizersCount ?: 0,
                    currentParticipantsCount = dto.currentParticipantsCount ?: 0,
                    currentOrganizersCount = dto.currentOrganizersCount ?: 0
                )
            }.sortedBy { it.rawDate }
        }
    }

    override suspend fun getCoordinatorDashboardEvents(userId: Int): Result<List<Event>> = runCatching {
        coroutineScope {
            val createdDeferred = async {
                httpClient.get("events") {
                    parameter("creatorId", userId)
                    parameter("size", 150)
                }.body<PagedEventResponse>().content
            }

            val organizerDeferred = async {
                httpClient.get("events") {
                    parameter("isOrganizer", true)
                    parameter("isDraft", false)
                    parameter("size", 150)
                }.body<PagedEventResponse>().content
            }

            val sectorDeferred = async {
                httpClient.get("events") {
                    parameter("isResponsibleSector", true)
                    parameter("isDraft", false)
                    parameter("size", 150)
                }.body<PagedEventResponse>().content
            }

            // Отфильтровываем удаленные из каждого источника
            val created = createdDeferred.await().filter { it.isDeleted != true }
            val organized = organizerDeferred.await().filter { it.isDeleted != true }
            val sector = sectorDeferred.await().filter { it.isDeleted != true }

            val allDto = (created + organized + sector).distinctBy { it.id }
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

            allDto.map { dto ->
                val isOverdue = dto.dateOfEvent < today && !dto.isCompleted

                val badge = when {
                    organized.any { it.id == dto.id } -> "Вы организатор"
                    created.any { it.id == dto.id } -> "Вы создатель"
                    sector.any { it.id == dto.id } -> "Ваш сектор"
                    else -> null
                }

                Event(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description ?: "",
                    venue = dto.venue,
                    photoBase64 = dto.photo,
                    dateStrCard = formatEventDate(dto.dateOfEvent, dto.startTime, null),
                    dateStrDetails = formatEventDate(dto.dateOfEvent, dto.startTime, dto.endTime),
                    rawDate = dto.dateOfEvent,
                    eventCreatorId = dto.eventCreatorId,
                    isDraft = dto.isDraft,
                    isCompleted = dto.isCompleted,
                    isOverdue = isOverdue,
                    relationBadge = badge,
                    isPublic = dto.isPublic,
                    organizers = dto.organizers?.map { orgDto ->
                        com.example.ais_sst_mobile.domain.model.Organizer(
                            userId = orgDto.userId,
                            userName = orgDto.userName,
                            userSurname = orgDto.userSurname,
                            userPhoto = orgDto.userPhoto
                        )
                    } ?: emptyList(),
                    isFreeEvent = dto.isFreeEvent ?: false,
                    maxParticipantsCount = dto.maxParticipantsCount ?: 0,
                    maxOrganizersCount = dto.maxOrganizersCount ?: 0,
                    currentParticipantsCount = dto.currentParticipantsCount ?: 0,
                    currentOrganizersCount = dto.currentOrganizersCount ?: 0
                )
            }.sortedBy { it.rawDate }
        }
    }

    override suspend fun getGlobalRoles(): Result<List<RoleDto>> = runCatching {
        httpClient.get("roles").body()
    }

    override suspend fun createEvent(request: CreateEventRequestDto): Result<EventDto> = runCatching {
        val response = httpClient.post("events") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status.isSuccess()) {
            response.body<EventDto>()
        } else {
            throw Exception("Ошибка сервера (${response.status.value}). Мероприятие не создано.")
        }
    }

    override suspend fun addOrganizer(eventId: Int, userId: Int): Result<Unit> = runCatching {
        httpClient.post("events/$eventId/organizers/$userId")
    }

    override suspend fun createEventRole(request: CreateEventRoleRequestDto): Result<Unit> = runCatching {
        httpClient.post("event-roles") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun createOrganizerApplication(eventId: Int): Result<Unit> = runCatching {
        val response = httpClient.post("role-applications/$eventId/orgainizer") {
            headers {
                remove(HttpHeaders.ContentType)
                remove(HttpHeaders.ContentLength)
            }
        }
        if (!response.status.isSuccess()) {
            throw Exception("Ошибка сервера (${response.status.value}). Не удалось открыть заявку на организатора.")
        }
    }

    override suspend fun deleteEvent(eventId: Int): Result<Unit> = runCatching {
        val response = httpClient.delete("events/$eventId")
        if (response.status.isSuccess()) {
            hasDeletedEventSignal = true
        } else {
            throw Exception("Ошибка сервера (${response.status.value}). Не удалось удалить мероприятие.")
        }
    }
}