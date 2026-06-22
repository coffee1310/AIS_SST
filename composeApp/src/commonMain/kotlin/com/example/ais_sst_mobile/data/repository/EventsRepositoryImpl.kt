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
import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import kotlinx.coroutines.IO
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

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
        val response = withContext(Dispatchers.IO) {
            httpClient.get("events") {
                parameter("dateFrom", dateFrom)
                parameter("dateTo", dateTo)
                parameter("size", 150)
                parameter("isDraft", false)
                parameter("sortBy", "dateOfEvent")
                parameter("sortDirection", "ASC")
            }.body<PagedEventResponse>()
        }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        withContext(Dispatchers.Default) {
            response.content.filter { it.isDeleted != true && it.dateOfEvent >= today }.map { dto ->
                mapToEvent(dto, today)
            }.sortedBy { it.rawDate }
        }
    }

    override suspend fun getEventRoles(eventId: Int): Result<List<EventRoleDto>> = runCatching {
        httpClient.get("event-roles") {
            parameter("eventId", eventId)
        }.body<PagedEventRoleResponse>().content
    }

    override suspend fun getEventById(id: Int): Result<Event> = runCatching {
        val dto = withContext(Dispatchers.IO) {
            httpClient.get("events/$id").body<EventDto>()
        }
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        withContext(Dispatchers.Default) {
            mapToEvent(dto, today)
        }
    }

    override suspend fun getAvailableEvents(userSectorsSafe: List<String>): Result<List<Event>> = runCatching {
        coroutineScope {
            // 1. Абсолютно все мероприятия (без фильтров, чтобы не зависеть от сломанного бэкенда)
            val allEventsDeferred = async(Dispatchers.IO) {
                runCatching {
                    httpClient.get("events") {
                        parameter("isDraft", false)
                        parameter("size", 150)
                    }.body<PagedEventResponse>().content
                }.getOrDefault(emptyList())
            }

            // 2. Все неудаленные роли
            val allRolesDeferred = async(Dispatchers.IO) {
                runCatching {
                    httpClient.get("event-roles") {
                        parameter("isDeleted", false)
                        parameter("page", 0)
                        parameter("size", 1000)
                    }.body<PagedEventRoleResponse>().content
                }.getOrDefault(emptyList())
            }

            // 3. Справочник глобальных ролей
            val globalRolesDeferred = async(Dispatchers.IO) {
                runCatching {
                    httpClient.get("roles").body<List<RoleDto>>()
                }.getOrDefault(emptyList())
            }

            val allEvents = allEventsDeferred.await()
            val allRoles = allRolesDeferred.await()
            val globalRoles = globalRolesDeferred.await()

            // ЛОКАЛЬНАЯ ФИЛЬТРАЦИЯ РОЛЕЙ: Оставляем только те, чей сектор есть у активиста
            val myRoles = allRoles.filter { role ->
                val globalRole = globalRoles.find { it.id == role.globalEventRoleId }
                val roleSectorSafe = globalRole?.sectorTitle?.trim()?.lowercase()
                roleSectorSafe != null && userSectorsSafe.contains(roleSectorSafe)
            }

            val roleEventIds = myRoles.map { it.eventId }.toSet()

            val combinedEvents = allEvents.toMutableList()
            val knownEventIds = combinedEvents.map { it.id }.toSet()

            val missingEventIds = roleEventIds - knownEventIds

            val missingEventsDeferred = missingEventIds.map { id ->
                async(Dispatchers.IO) {
                    runCatching {
                        httpClient.get("events/$id").body<EventDto>()
                    }.getOrNull()
                }
            }

            missingEventsDeferred.forEach { deferred ->
                deferred.await()?.let { combinedEvents.add(it) }
            }

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

            withContext(Dispatchers.Default) {
                combinedEvents.filter { dto ->
                    val isPub = dto.isPublic
                    val isLookingForOrg = (dto.maxOrganizersCount ?: 0) > (dto.currentOrganizersCount ?: 0)

                    // БЕЗОПАСНАЯ ЛОКАЛЬНАЯ ПРОВЕРКА ДЛЯ ЗАКРЫТЫХ МЕРОПРИЯТИЙ (Свободных)
                    val eventSectorsSafe = dto.sectors?.map { it.title.trim().lowercase() } ?: emptyList()
                    val isMySectorLocal = eventSectorsSafe.any { it in userSectorsSafe }
                    val isFreeMySector = dto.isFreeEvent == true && isMySectorLocal

                    val hasRoleForMe = roleEventIds.contains(dto.id)

                    val isVisible = isPub || isLookingForOrg || isFreeMySector || hasRoleForMe

                    dto.isDeleted != true && !dto.isDraft && dto.dateOfEvent >= today && isVisible
                }.map { mapToEvent(it, today) }.sortedBy { it.rawDate }
            }
        }
    }

    override suspend fun getCoordinatorDashboardEvents(userId: Int): Result<List<Event>> = runCatching {
        coroutineScope {
            val createdDeferred = async(Dispatchers.IO) {
                httpClient.get("events") {
                    parameter("creatorId", userId)
                    parameter("size", 150)
                    parameter("sortBy", "dateOfEvent")
                    parameter("sortDirection", "DESC")
                }.body<PagedEventResponse>().content
            }

            val organizerDeferred = async(Dispatchers.IO) {
                httpClient.get("events") {
                    parameter("isOrganizer", true)
                    parameter("size", 150)
                    parameter("sortBy", "dateOfEvent")
                    parameter("sortDirection", "DESC")
                }.body<PagedEventResponse>().content
            }

            val sectorDeferred = async(Dispatchers.IO) {
                httpClient.get("events") {
                    parameter("isMySector", true)
                    parameter("size", 150)
                    parameter("sortBy", "dateOfEvent")
                    parameter("sortDirection", "DESC")
                }.body<PagedEventResponse>().content
            }

            val created = createdDeferred.await().filter { it.isDeleted != true }
            val organized = organizerDeferred.await().filter { it.isDeleted != true }

            // ИСПРАВЛЕНИЕ ЛОГИКИ: Если мероприятие Свободное и открыто для всех секторов (sectorTitle пустой),
            // мы не выводим его на дашборд с плашкой "Ваш сектор"
            val sector = sectorDeferred.await().filter { dto ->
                dto.isDeleted != true && !(dto.isFreeEvent == true && dto.sectorTitle.isNullOrBlank())
            }

            val allDto = (created + organized + sector).distinctBy { it.id }
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

            withContext(Dispatchers.Default) {
                allDto.map { dto ->
                    val badge = when {
                        organized.any { it.id == dto.id } -> "Вы организатор"
                        created.any { it.id == dto.id } -> "Вы создатель"
                        sector.any { it.id == dto.id } -> "Ваш сектор"
                        else -> null
                    }
                    mapToEvent(dto, today, badge)
                }.sortedBy { it.rawDate }
            }
        }
    }

    override suspend fun getChairmanDashboardEvents(userId: Int): Result<List<Event>> = runCatching {
        val response = withContext(Dispatchers.IO) {
            httpClient.get("events") {
                parameter("size", 150)
                parameter("sortBy", "dateOfEvent")
                parameter("sortDirection", "DESC")
            }.body<PagedEventResponse>()
        }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        withContext(Dispatchers.Default) {
            response.content
                .filter { it.isDeleted != true && it.isCompleted != true }
                .map { dto ->
                    val badge = when {
                        dto.eventCreatorId == userId -> "Вы создатель"
                        dto.organizers?.any { it.userId == userId } == true -> "Вы организатор"
                        else -> null
                    }
                    mapToEvent(dto, today, badge)
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

    // --- Вспомогательные методы ---

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

    private fun mapToEvent(dto: EventDto, today: String, relationBadge: String? = null): Event {
        return Event(
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
            relationBadge = relationBadge,
            isFreeEvent = dto.isFreeEvent ?: false,
            maxParticipantsCount = dto.maxParticipantsCount ?: 0,
            maxOrganizersCount = dto.maxOrganizersCount ?: 0,
            currentParticipantsCount = dto.currentParticipantsCount ?: 0,
            currentOrganizersCount = dto.currentOrganizersCount ?: 0,
            sectorTitle = dto.sectorTitle,
            isMySector = dto.isMySector ?: false
        )
    }
}