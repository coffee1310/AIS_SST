package com.example.ais_sst_mobile.domain.repository

import com.example.ais_sst_mobile.data.network.dto.CreateEventRequestDto
import com.example.ais_sst_mobile.data.network.dto.CreateEventRoleRequestDto
import com.example.ais_sst_mobile.data.network.dto.EventDto
import com.example.ais_sst_mobile.data.network.dto.EventRoleDto
import com.example.ais_sst_mobile.data.network.dto.ParticipantSlotsDto
import com.example.ais_sst_mobile.data.network.dto.RoleDto
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.data.network.dto.OrganizerApplicationDto

interface EventsRepository {
    fun getAndClearDeletedEventSignal(): Boolean
    suspend fun getUpcomingEvents(dateFrom: String, dateTo: String): Result<List<Event>>
    suspend fun getAvailableEvents(): Result<List<Event>>
    suspend fun getCoordinatorDashboardEvents(userId: Int): Result<List<Event>>
    suspend fun getChairmanDashboardEvents(userId: Int): Result<List<Event>>
    suspend fun getEventById(id: Int): Result<Event>
    suspend fun getGlobalRoles(): Result<List<RoleDto>>
    suspend fun getEventRoles(eventId: Int): Result<List<EventRoleDto>>

    suspend fun createEvent(request: CreateEventRequestDto): Result<EventDto>
    suspend fun updateEvent(eventId: Int, request: CreateEventRequestDto): Result<EventDto>
    suspend fun deleteEvent(eventId: Int): Result<Unit>

    suspend fun addOrganizer(eventId: Int, userId: Int): Result<Unit>
    suspend fun removeOrganizer(eventId: Int, userId: Int): Result<Unit>
    suspend fun createOrganizerApplication(eventId: Int): Result<Unit>

    suspend fun createEventRole(request: CreateEventRoleRequestDto): Result<Unit>
    suspend fun updateEventRole(roleId: Int, request: CreateEventRoleRequestDto): Result<Unit>
    suspend fun deleteEventRole(roleId: Int): Result<Unit>

    suspend fun applyForEventRole(roleId: Int, description: String): Result<Unit>
    suspend fun joinEventAsParticipant(eventId: Int): Result<Unit>
    suspend fun getEventSlots(eventId: Int): Result<ParticipantSlotsDto>
    suspend fun getMyRoleApplications(): Result<List<com.example.ais_sst_mobile.data.network.dto.RoleApplicationDto>>
    suspend fun getFilteredRoleApplications(userId: Int, status: String? = null): Result<List<com.example.ais_sst_mobile.data.network.dto.RoleApplicationDto>>
    suspend fun getMyParticipantEvents(): Result<List<com.example.ais_sst_mobile.data.network.dto.MyEventParticipantDto>>
    suspend fun leaveEventParticipant(participantId: Int): Result<Unit>
    suspend fun getMyOrganizerApplications(): Result<List<OrganizerApplicationDto>>}