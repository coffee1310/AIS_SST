package com.example.ais_sst_mobile.presentation.home.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.EventRoleDto
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.model.Organizer
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface EventDetailsEffect {
    data object NavigateBack : EventDetailsEffect
    data class ShowError(val message: String) : EventDetailsEffect
}

sealed interface CoordinatorEventDetailsState {
    data object Loading : CoordinatorEventDetailsState
    data class Success(
        val event: Event,
        val roles: List<EventRoleDto>,
        val creator: Organizer?,
        val showApplications: Boolean,
        val showManagementSection: Boolean,
        val showEditButton: Boolean,
        val showFinishButton: Boolean,
        val showDeleteButton: Boolean,
        val showReportButton: Boolean
    ) : CoordinatorEventDetailsState
    data class Error(val message: String) : CoordinatorEventDetailsState
}

class EventDetailsScreenModel(
    private val eventsRepository: EventsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CoordinatorEventDetailsState>(CoordinatorEventDetailsState.Loading)
    val state: StateFlow<CoordinatorEventDetailsState> = _state.asStateFlow()

    private val _effect = Channel<EventDetailsEffect>()
    val effect = _effect.receiveAsFlow()

    fun loadEvent(eventId: Int, activeRole: AppRole) {
        viewModelScope.launch {
            _state.value = CoordinatorEventDetailsState.Loading

            val eventResult = eventsRepository.getEventById(eventId)
            val rolesResult = eventsRepository.getEventRoles(eventId)
            val currentUserResult = userRepository.getUserProfile()

            if (eventResult.isSuccess && rolesResult.isSuccess && currentUserResult.isSuccess) {
                var event = eventResult.getOrNull()!!
                var roles = rolesResult.getOrNull()!!
                val currentUser = currentUserResult.getOrNull()!!

                var creator: Organizer? = null
                val creatorRes = userRepository.getUserProfileById(event.eventCreatorId)
                if (creatorRes.isSuccess) {
                    val cUser = creatorRes.getOrNull()!!

                    // Проверяем роль создателя, используя строковое поле role от сервера
                    val isCreatorCurator = AppRole.fromServerName(cUser.role) == AppRole.CURATOR

                    val groupInfo = if (isCreatorCurator) {
                        "куратор Студенческого совета"
                    } else if (cUser.courseNumber != null && cUser.specialityShortTitle != null && cUser.groupName != null) {
                        "студент группы ${cUser.courseNumber}${cUser.specialityShortTitle}-${cUser.groupName}"
                    } else if (cUser.groupName != null) {
                        "студент группы ${cUser.groupName}"
                    } else {
                        "создатель мероприятия"
                    }
                    creator = Organizer(
                        userId = cUser.id,
                        userName = cUser.name,
                        userSurname = cUser.surname,
                        userPhoto = cUser.photo,
                        groupInfo = groupInfo
                    )
                }

                if (event.organizers.isNotEmpty()) {
                    val enrichedOrganizers = event.organizers.map { org ->
                        val userRes = userRepository.getUserProfileById(org.userId)
                        if (userRes.isSuccess) {
                            val user = userRes.getOrNull()!!
                            val groupInfo = if (user.courseNumber != null && user.specialityShortTitle != null && user.groupName != null) {
                                "студент группы ${user.courseNumber}${user.specialityShortTitle}-${user.groupName}"
                            } else if (user.groupName != null) {
                                "студент группы ${user.groupName}"
                            } else {
                                "Организатор мероприятия"
                            }
                            org.copy(groupInfo = groupInfo, userPhoto = user.photo ?: org.userPhoto)
                        } else { org }
                    }
                    event = event.copy(organizers = enrichedOrganizers)
                }

                var showApplications = true
                var showManagementSection = false
                var showEditButton = false
                var showFinishButton = false
                var showDeleteButton = false
                var showReportButton = false

                val isEventCreator = currentUser.id == event.eventCreatorId
                val isOrganizer = event.organizers.any { it.userId == currentUser.id }

                // Полный доступ для ТОП-состава
                if (activeRole == AppRole.CHAIRMAN || activeRole == AppRole.DEPUTY_CHAIRMAN || activeRole == AppRole.CURATOR) {
                    showManagementSection = true
                    showEditButton = true
                    showFinishButton = event.isOverdue
                    showDeleteButton = true
                }
                // Доступ для Секретаря
                else if (activeRole == AppRole.SECRETARY) {
                    showManagementSection = true
                    showReportButton = true
                    if (isOrganizer) {
                        showEditButton = true
                    }
                }
                // Логика для Координатора
                else if (currentUser.coordinatorSectorId != null) {
                    val dashboardEvents = eventsRepository.getCoordinatorDashboardEvents(currentUser.id).getOrNull() ?: emptyList()
                    val badge = dashboardEvents.find { it.id == eventId }?.relationBadge

                    val hasFullAccess = badge == "Вы организатор" || badge == "Вы создатель"
                    val isSectorAccess = badge == "Ваш сектор"

                    if (hasFullAccess) {
                        showManagementSection = true
                        showEditButton = true
                        showFinishButton = event.isOverdue
                        showDeleteButton = isEventCreator
                    } else if (isSectorAccess) {
                        // Показываем заявки, но фильтруем роли только для своего сектора
                        showApplications = true
                        val globalRolesRes = eventsRepository.getGlobalRoles()
                        if (globalRolesRes.isSuccess) {
                            val globalRolesList = globalRolesRes.getOrNull() ?: emptyList()
                            val mySectorId = currentUser.coordinatorSectorId
                            val myGlobalRoleIds = globalRolesList.filter { it.sectorId == mySectorId }.map { it.id }
                            roles = roles.filter { it.globalEventRoleId in myGlobalRoleIds }
                        }
                    } else {
                        // Если координатор зашел в чужое закрытое мероприятие или Свободное для всех
                        if (event.isFreeEvent && event.sectorTitle.isNullOrBlank()) {
                            showApplications = true
                            roles = emptyList() // Убираем чужие роли
                        } else {
                            showApplications = false
                        }
                    }
                }

                _state.value = CoordinatorEventDetailsState.Success(
                    event = event,
                    roles = roles,
                    creator = creator,
                    showApplications = showApplications,
                    showManagementSection = showManagementSection,
                    showEditButton = showEditButton,
                    showFinishButton = showFinishButton,
                    showDeleteButton = showDeleteButton,
                    showReportButton = showReportButton
                )
            } else {
                _state.value = CoordinatorEventDetailsState.Error("Не удалось загрузить данные мероприятия")
            }
        }
    }

    fun deleteEvent(eventId: Int) {
        viewModelScope.launch {
            _state.value = CoordinatorEventDetailsState.Loading
            eventsRepository.deleteEvent(eventId)
                .onSuccess {
                    _effect.send(EventDetailsEffect.NavigateBack)
                }
                .onFailure {
                    _state.value = CoordinatorEventDetailsState.Error("Не удалось удалить мероприятие")
                    _effect.send(EventDetailsEffect.ShowError("Ошибка при удалении. Проверьте подключение."))
                }
        }
    }
}