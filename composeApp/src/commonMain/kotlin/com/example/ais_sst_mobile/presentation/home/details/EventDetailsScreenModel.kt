package com.example.ais_sst_mobile.presentation.home.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.EventRoleDto
import com.example.ais_sst_mobile.domain.model.Event
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
        val showApplications: Boolean,
        val showManagementButtons: Boolean,
        val showDeleteButton: Boolean // <-- Флаг для кнопки удаления
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

    fun loadEvent(eventId: Int) {
        viewModelScope.launch {
            _state.value = CoordinatorEventDetailsState.Loading

            val eventResult = eventsRepository.getEventById(eventId)
            val rolesResult = eventsRepository.getEventRoles(eventId)
            val currentUserResult = userRepository.getUserProfile()

            if (eventResult.isSuccess && rolesResult.isSuccess && currentUserResult.isSuccess) {
                var event = eventResult.getOrNull()!!
                var roles = rolesResult.getOrNull()!!
                val currentUser = currentUserResult.getOrNull()!!

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

                            org.copy(
                                groupInfo = groupInfo,
                                userPhoto = user.photo ?: org.userPhoto
                            )
                        } else {
                            org
                        }
                    }
                    event = event.copy(organizers = enrichedOrganizers)
                }

                // --- ЛОГИКА ДОСТУПА К РОЛЯМ И УПРАВЛЕНИЮ ---
                var showApplications = true
                var showManagementButtons = false // По умолчанию кнопки скрыты для всех
                val showDeleteButton = currentUser.id == event.eventCreatorId // Только создатель может удалить

                // Если у пользователя есть сектор -> значит он координатор
                if (currentUser.coordinatorSectorId != null) {
                    val dashboardEvents = eventsRepository.getCoordinatorDashboardEvents(currentUser.id).getOrNull() ?: emptyList()
                    val badge = dashboardEvents.find { it.id == eventId }?.relationBadge

                    val hasFullAccess = badge == "Вы организатор" || badge == "Вы создатель"
                    val isSectorAccess = badge == "Ваш сектор"

                    if (hasFullAccess) {
                        showApplications = true
                        showManagementButtons = true // Открываем доступ к редактированию
                    } else if (isSectorAccess) {
                        // Скрываем общие заявки и кнопки редактирования (он не создатель)
                        showApplications = false
                        showManagementButtons = false

                        // Фильтруем роли: оставляем только те, которые относятся к сектору текущего координатора
                        val globalRoles = eventsRepository.getGlobalRoles().getOrNull() ?: emptyList()
                        val mySectorId = currentUser.coordinatorSectorId
                        val myGlobalRoleIds = globalRoles.filter { it.sectorId == mySectorId }.map { it.id }
                        roles = roles.filter { it.globalEventRoleId in myGlobalRoleIds }
                    } else {
                        // Просто зашел на чужое мероприятие посмотреть
                        showApplications = true
                        showManagementButtons = false
                    }
                }

                _state.value = CoordinatorEventDetailsState.Success(
                    event = event,
                    roles = roles,
                    showApplications = showApplications,
                    showManagementButtons = showManagementButtons,
                    showDeleteButton = showDeleteButton
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
                    // Возвращаем состояние ошибки (чтобы загрузка пропала)
                    _state.value = CoordinatorEventDetailsState.Error("Не удалось удалить мероприятие")
                    _effect.send(EventDetailsEffect.ShowError("Ошибка при удалении. Проверьте подключение."))
                }
        }
    }
}