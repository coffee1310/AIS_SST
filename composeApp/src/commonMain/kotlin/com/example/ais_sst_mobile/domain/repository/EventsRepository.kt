package com.example.ais_sst_mobile.domain.repository

import com.example.ais_sst_mobile.data.network.dto.CreateEventRequestDto
import com.example.ais_sst_mobile.data.network.dto.CreateEventRoleRequestDto
import com.example.ais_sst_mobile.data.network.dto.EventDto
import com.example.ais_sst_mobile.data.network.dto.EventRoleDto
import com.example.ais_sst_mobile.data.network.dto.RoleDto
import com.example.ais_sst_mobile.domain.model.Event

interface EventsRepository {
    suspend fun getUpcomingEvents(dateFrom: String, dateTo: String): Result<List<Event>>
    suspend fun getEventById(id: Int): Result<Event>
    suspend fun getAvailableEvents(): Result<List<Event>>
    suspend fun getCoordinatorDashboardEvents(userId: Int): Result<List<Event>>
    suspend fun getGlobalRoles(): Result<List<RoleDto>>
    suspend fun createEvent(request: CreateEventRequestDto): Result<EventDto>
    suspend fun addOrganizer(eventId: Int, userId: Int): Result<Unit>
    suspend fun createEventRole(request: CreateEventRoleRequestDto): Result<Unit>
    suspend fun getEventRoles(eventId: Int): Result<List<EventRoleDto>>
}