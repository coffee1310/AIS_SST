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
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class EventsRepositoryImpl(
    private val httpClient: HttpClient
) : EventsRepository {

    // Вспомогательная функция, защищающая от падений при 502 Bad Gateway
    private suspend inline fun <reified T> HttpResponse.safeBody(): T {
        if (this.status.isSuccess()) {
            return this.body<T>()
        } else {
            throw Exception("Сервер временно недоступен (${this.status.value}). Пожалуйста, попробуйте позже.")
        }
    }

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
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<PagedEventResponse>()
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
            headers { append(HttpHeaders.Connection, "close") }
        }.safeBody<PagedEventRoleResponse>().content
    }

    override suspend fun getEventById(id: Int): Result<Event> = runCatching {
        val dto = withContext(Dispatchers.IO) {
            httpClient.get("events/$id") {
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<EventDto>()
        }
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        withContext(Dispatchers.Default) {
            mapToEvent(dto, today)
        }
    }

    override suspend fun getAvailableEvents(): Result<List<Event>> = runCatching {
        val publicEvents = runCatching {
            httpClient.get("events") {
                parameter("isPublic", true)
                parameter("isDraft", false)
                parameter("size", 150)
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<PagedEventResponse>().content
        }.getOrDefault(emptyList())

        delay(500)

        val mySectorEvents = runCatching {
            httpClient.get("events") {
                parameter("isMySector", true)
                parameter("isDraft", false)
                parameter("size", 150)
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<PagedEventResponse>().content
        }.getOrDefault(emptyList())

        delay(500)

        val allEvents = runCatching {
            httpClient.get("events") {
                parameter("isDraft", false)
                parameter("size", 150)
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<PagedEventResponse>().content
        }.getOrDefault(emptyList())

        delay(500)

        val myRoles = runCatching {
            httpClient.get("event-roles") {
                parameter("isMySector", true)
                parameter("isDeleted", false)
                parameter("page", 0)
                parameter("size", 1000)
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<PagedEventRoleResponse>().content
        }.getOrDefault(emptyList())

        val roleEventIds = myRoles.map { it.eventId }.toSet()
        val combinedEvents = (publicEvents + mySectorEvents + allEvents).distinctBy { it.id }.toMutableList()
        val knownEventIds = combinedEvents.map { it.id }.toSet()
        val missingEventIds = roleEventIds - knownEventIds

        for (id in missingEventIds) {
            delay(300)
            val event = runCatching {
                httpClient.get("events/$id") {
                    headers { append(HttpHeaders.Connection, "close") }
                }.safeBody<EventDto>()
            }.getOrNull()
            if (event != null) {
                combinedEvents.add(event)
            }
        }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        withContext(Dispatchers.Default) {
            combinedEvents.filter { dto ->
                val isPub = dto.isPublic
                val isLookingForOrg = (dto.maxOrganizersCount ?: 0) > (dto.currentOrganizersCount ?: 0)
                val isFreeMySector = dto.isFreeEvent == true && dto.isMySector == true
                val hasRoleForMe = roleEventIds.contains(dto.id)
                val isVisible = isPub || isLookingForOrg || isFreeMySector || hasRoleForMe

                dto.isDeleted != true && !dto.isDraft && dto.dateOfEvent >= today && isVisible
            }.map { mapToEvent(it, today) }.sortedBy { it.rawDate }
        }
    }

    override suspend fun getCoordinatorDashboardEvents(userId: Int): Result<List<Event>> = runCatching {
        val created = runCatching {
            httpClient.get("events") {
                parameter("creatorId", userId)
                parameter("size", 150)
                parameter("sortBy", "dateOfEvent")
                parameter("sortDirection", "DESC")
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<PagedEventResponse>().content
        }.getOrDefault(emptyList()).filter { it.isDeleted != true }

        delay(500)

        val organized = runCatching {
            httpClient.get("events") {
                parameter("isOrganizer", true)
                parameter("size", 150)
                parameter("sortBy", "dateOfEvent")
                parameter("sortDirection", "DESC")
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<PagedEventResponse>().content
        }.getOrDefault(emptyList()).filter { it.isDeleted != true }

        delay(500)

        val sector = runCatching {
            httpClient.get("events") {
                parameter("isMySector", true)
                parameter("size", 150)
                parameter("sortBy", "dateOfEvent")
                parameter("sortDirection", "DESC")
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<PagedEventResponse>().content
        }.getOrDefault(emptyList()).filter { dto ->
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

    override suspend fun getChairmanDashboardEvents(userId: Int): Result<List<Event>> = runCatching {
        val response = withContext(Dispatchers.IO) {
            httpClient.get("events") {
                parameter("size", 150)
                parameter("sortBy", "dateOfEvent")
                parameter("sortDirection", "DESC")
                headers { append(HttpHeaders.Connection, "close") }
            }.safeBody<PagedEventResponse>()
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
        httpClient.get("roles") {
            headers { append(HttpHeaders.Connection, "close") }
        }.safeBody()
    }

    override suspend fun createEvent(request: CreateEventRequestDto): Result<EventDto> = runCatching {
        val response = httpClient.post("events") {
            contentType(ContentType.Application.Json)
            setBody(request)
            headers { append(HttpHeaders.Connection, "close") }
        }

        if (response.status.isSuccess()) {
            response.body<EventDto>()
        } else {
            throw Exception("Ошибка сервера (${response.status.value}). Мероприятие не создано.")
        }
    }

    // --- НОВЫЕ МЕТОДЫ ДЛЯ РЕДАКТИРОВАНИЯ МЕРОПРИЯТИЙ И РОЛЕЙ ---

    override suspend fun updateEvent(eventId: Int, request: CreateEventRequestDto): Result<EventDto> = runCatching {
        val response = httpClient.put("events/$eventId") {
            contentType(ContentType.Application.Json)
            setBody(request)
            headers { append(HttpHeaders.Connection, "close") }
        }
        if (response.status.isSuccess()) {
            response.body<EventDto>()
        } else {
            throw Exception("Ошибка сервера (${response.status.value}). Мероприятие не обновлено.")
        }
    }

    override suspend fun addOrganizer(eventId: Int, userId: Int): Result<Unit> = runCatching {
        val response = httpClient.post("events/$eventId/organizers/$userId") {
            headers { append(HttpHeaders.Connection, "close") }
        }
        if (!response.status.isSuccess()) throw Exception("Не удалось добавить организатора")
    }

    override suspend fun removeOrganizer(eventId: Int, userId: Int): Result<Unit> = runCatching {
        val response = httpClient.delete("events/$eventId/organizers/$userId") {
            headers { append(HttpHeaders.Connection, "close") }
        }
        if (!response.status.isSuccess()) throw Exception("Не удалось удалить организатора")
    }

    override suspend fun createEventRole(request: CreateEventRoleRequestDto): Result<Unit> = runCatching {
        val response = httpClient.post("event-roles") {
            contentType(ContentType.Application.Json)
            setBody(request)
            headers { append(HttpHeaders.Connection, "close") }
        }
        if (!response.status.isSuccess()) throw Exception("Не удалось создать роль")
    }

    override suspend fun updateEventRole(roleId: Int, request: CreateEventRoleRequestDto): Result<Unit> = runCatching {
        val response = httpClient.put("event-roles/$roleId") {
            contentType(ContentType.Application.Json)
            setBody(request)
            headers { append(HttpHeaders.Connection, "close") }
        }
        if (!response.status.isSuccess()) throw Exception("Не удалось обновить роль")
    }

    override suspend fun deleteEventRole(roleId: Int): Result<Unit> = runCatching {
        val response = httpClient.delete("event-roles/$roleId") {
            headers { append(HttpHeaders.Connection, "close") }
        }
        if (!response.status.isSuccess()) throw Exception("Ошибка удаления роли")
    }

    // -----------------------------------------------------------

    override suspend fun createOrganizerApplication(eventId: Int): Result<Unit> = runCatching {
        val response = httpClient.post("role-applications/$eventId/orgainizer") {
            headers {
                remove(HttpHeaders.ContentType)
                remove(HttpHeaders.ContentLength)
                append(HttpHeaders.Connection, "close")
            }
        }
        if (!response.status.isSuccess()) {
            throw Exception("Ошибка сервера (${response.status.value}). Не удалось открыть заявку на организатора.")
        }
    }

    override suspend fun deleteEvent(eventId: Int): Result<Unit> = runCatching {
        val response = httpClient.delete("events/$eventId") {
            headers { append(HttpHeaders.Connection, "close") }
        }
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
            rawStartTime = dto.startTime, // ДОБАВЛЕНО для формы редактирования
            rawEndTime = dto.endTime, // ДОБАВЛЕНО для формы редактирования
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
            isMySector = dto.isMySector ?: false,
            sectorIds = dto.sectors?.map { it.id } ?: emptyList() // ДОБАВЛЕНО для формы редактирования
        )
    }
}