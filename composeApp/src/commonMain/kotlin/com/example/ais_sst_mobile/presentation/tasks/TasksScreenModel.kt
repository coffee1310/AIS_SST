package com.example.ais_sst_mobile.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.network.dto.MyEventParticipantDto
import com.example.ais_sst_mobile.data.network.dto.OrganizerApplicationDto
import com.example.ais_sst_mobile.data.network.dto.RoleApplicationDto
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventTaskGroup(
    val eventId: Int,
    val eventTitle: String,
    val eventPhoto: String? = null,
    val roles: List<RoleApplicationDto> = emptyList(),
    val participantInfo: MyEventParticipantDto? = null
)

sealed interface TasksState {
    data object Loading : TasksState
    data class Success(val groups: List<EventTaskGroup>) : TasksState
    data class Error(val message: String) : TasksState
}

class TasksScreenModel(
    private val eventsRepository: EventsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _tasksCache = mutableMapOf<Int, List<EventTaskGroup>>()

    private val _rawState = MutableStateFlow<TasksState>(TasksState.Loading)

    val state = combine(_rawState, _searchQuery) { currentState, query ->
        if (currentState is TasksState.Success) {
            val filtered = if (query.isBlank()) {
                currentState.groups
            } else {
                currentState.groups.filter { it.eventTitle.contains(query, ignoreCase = true) }
            }
            TasksState.Success(filtered)
        } else {
            currentState
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, TasksState.Loading)

    init {
        loadDataForTab(0)
    }

    fun selectTab(index: Int) {
        _searchQuery.value = ""
        _selectedTab.value = index
        loadDataForTab(index)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun loadDataForTab(tabIndex: Int, forceRefresh: Boolean = false) {
        val cached = _tasksCache[tabIndex]
        if (cached != null && !forceRefresh) {
            _rawState.value = TasksState.Success(cached)
            return
        }

        viewModelScope.launch {
            val userId = sessionManager.fetchUserId()
            if (userId == null) {
                _rawState.value = TasksState.Error("Ошибка авторизации")
                return@launch
            }

            _rawState.value = TasksState.Loading

            try {
                val resultList = when (tabIndex) {
                    0 -> {
                        // На рассмотрении: обычные роли + заявки на организатора
                        val rolesDeferred = async {
                            eventsRepository.getFilteredRoleApplications(userId, "НА_РАССМОТРЕНИИ")
                        }
                        val organizerDeferred = async {
                            eventsRepository.getMyOrganizerApplications()
                        }

                        val roles = rolesDeferred.await().getOrNull() ?: emptyList()
                        val organizerApps = organizerDeferred.await().getOrNull() ?: emptyList()

                        groupDataWithOrganizer(roles, organizerApps)
                    }
                    1 -> {
                        val rolesDeferred = async { eventsRepository.getFilteredRoleApplications(userId, "ОДОБРЕНА") }
                        val partsDeferred = async { eventsRepository.getMyParticipantEvents() }

                        val roles = rolesDeferred.await().getOrNull() ?: emptyList()
                        val parts = partsDeferred.await().getOrNull() ?: emptyList()
                        groupData(roles, parts)
                    }
                    2 -> {
                        val res = eventsRepository.getFilteredRoleApplications(userId, "ОТКЛОНЕНА")
                        groupData(res.getOrNull() ?: emptyList())
                    }
                    else -> emptyList()
                }

                val enrichedList = enrichGroupsWithPhotos(resultList)
                _tasksCache[tabIndex] = enrichedList
                _rawState.value = TasksState.Success(enrichedList)

            } catch (e: Exception) {
                _rawState.value = TasksState.Error("Ошибка загрузки. Проверьте сеть")
            }
        }
    }

    private suspend fun enrichGroupsWithPhotos(groups: List<EventTaskGroup>): List<EventTaskGroup> {
        return groups.map { group ->
            if (!group.eventPhoto.isNullOrBlank()) {
                group
            } else {
                try {
                    val event = eventsRepository.getEventById(group.eventId).getOrNull()
                    group.copy(eventPhoto = event?.photoBase64)
                } catch (_: Exception) {
                    group
                }
            }
        }
    }

    private fun groupData(
        roles: List<RoleApplicationDto>,
        participants: List<MyEventParticipantDto> = emptyList()
    ): List<EventTaskGroup> {
        val map = mutableMapOf<Int, EventTaskGroup>()

        roles.forEach { role ->
            val group = map.getOrPut(role.eventId) {
                EventTaskGroup(role.eventId, role.eventTitle, role.eventPhoto)
            }
            map[role.eventId] = group.copy(roles = group.roles + role)
        }

        participants.forEach { part ->
            val group = map.getOrPut(part.eventId) {
                EventTaskGroup(part.eventId, part.eventTitle, part.eventPhoto)
            }
            map[part.eventId] = group.copy(participantInfo = part)
        }

        return map.values.toList().sortedByDescending {
            it.roles.maxOfOrNull { r -> r.createdAt ?: "" } ?: it.participantInfo?.joinedAt ?: ""
        }
    }

    /**
     * Группировка обычных ролей + заявок на организатора
     */
    private fun groupDataWithOrganizer(
        roles: List<RoleApplicationDto>,
        organizerApps: List<OrganizerApplicationDto>
    ): List<EventTaskGroup> {
        val map = mutableMapOf<Int, EventTaskGroup>()

        // Обычные роли
        roles.forEach { role ->
            val group = map.getOrPut(role.eventId) {
                EventTaskGroup(role.eventId, role.eventTitle, role.eventPhoto)
            }
            map[role.eventId] = group.copy(roles = group.roles + role)
        }

        // Заявки на организатора
        organizerApps.forEach { orgApp ->
            val group = map.getOrPut(orgApp.eventId) {
                EventTaskGroup(orgApp.eventId, orgApp.eventTitle)
            }

            // Создаём фейковую роль "Организатор" для отображения
            val fakeOrganizerRole = RoleApplicationDto(
                id = orgApp.id,
                eventRoleId = -1,
                eventRoleTitle = "Организатор",
                eventId = orgApp.eventId,
                eventTitle = orgApp.eventTitle,
                status = orgApp.status,
                createdAt = orgApp.createdAt,
                description = null,
                rejectionReason = null,
                eventPhoto = null,
                roleDeadline = null,
                isReserve = null,
                updatedAt = null,
                studentId = null,
                studentName = null,
                studentSurname = null,
                studentPatronymic = null,
                studentEmail = null,
                sectorParticipantId = null,
                sectorTitle = null
            )

            map[orgApp.eventId] = group.copy(roles = group.roles + fakeOrganizerRole)
        }

        return map.values.toList().sortedByDescending {
            it.roles.maxOfOrNull { r -> r.createdAt ?: "" } ?: ""
        }
    }

    fun leaveParticipantRole(participantId: Int) {
        viewModelScope.launch {
            val result = eventsRepository.leaveEventParticipant(participantId)

            if (result.isSuccess) {
                _tasksCache.clear()
                loadDataForTab(_selectedTab.value, forceRefresh = true)
            } else {
                // Даже при ошибке обновляем список
                _tasksCache.clear()
                loadDataForTab(_selectedTab.value, forceRefresh = true)
            }
        }
    }
}