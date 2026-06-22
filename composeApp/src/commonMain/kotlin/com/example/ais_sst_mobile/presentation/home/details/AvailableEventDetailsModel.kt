package com.example.ais_sst_mobile.presentation.home.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.EventRoleDto
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.model.Organizer
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AvailableEventDetailsState {
    data object Loading : AvailableEventDetailsState
    data class Success(
        val event: Event,
        val roles: List<EventRoleDto>,
        val creator: Organizer?,
        val showOrganizerCard: Boolean,
        val showParticipantCard: Boolean
    ) : AvailableEventDetailsState
    data class Error(val message: String) : AvailableEventDetailsState
}

class AvailableEventDetailsScreenModel(
    private val eventsRepository: EventsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AvailableEventDetailsState>(AvailableEventDetailsState.Loading)
    val state: StateFlow<AvailableEventDetailsState> = _state.asStateFlow()

    fun loadEvent(eventId: Int) {
        viewModelScope.launch {
            _state.value = AvailableEventDetailsState.Loading

            val eventResult = eventsRepository.getEventById(eventId)
            val rolesResult = eventsRepository.getEventRoles(eventId)
            val userProfileResult = userRepository.getUserProfile()
            val globalRolesResult = eventsRepository.getGlobalRoles()

            if (eventResult.isSuccess && rolesResult.isSuccess && userProfileResult.isSuccess) {
                var event = eventResult.getOrNull()!!
                val allRoles = rolesResult.getOrNull()!!
                val currentUser = userProfileResult.getOrNull()!!
                val globalRoles = globalRolesResult.getOrNull() ?: emptyList()

                // --- ЛОГИКА ФИЛЬТРАЦИИ РОЛЕЙ И КАРТОЧЕК ---

                // 1. Карточка Организатора
                val showOrganizerCard = event.maxOrganizersCount > event.currentOrganizersCount

                // 2. Карточка Участника (Свободное И (Публичное ИЛИ это мой сектор))
                val showParticipantCard = event.isFreeEvent && (event.isPublic || event.isMySector)

                // 3. Остальные роли (БЕЗОПАСНОЕ СРАВНЕНИЕ)
                // Приводим все сектора пользователя к нижнему регистру без пробелов
                val userSectorsSafe = currentUser.userSectors.map { it.trim().lowercase() }

                val filteredRoles = if (event.isPublic) {
                    allRoles // Если публичное - видны все роли
                } else {
                    // Если закрытое - оставляем только те роли, сектор которых есть у активиста
                    allRoles.filter { role ->
                        val globalRole = globalRoles.find { it.id == role.globalEventRoleId }
                        // Безопасно получаем сектор глобальной роли
                        val roleSectorSafe = globalRole?.sectorTitle?.trim()?.lowercase()

                        roleSectorSafe != null && userSectorsSafe.contains(roleSectorSafe)
                    }
                }

                var creator: Organizer? = null
                val creatorRes = userRepository.getUserProfileById(event.eventCreatorId)
                if (creatorRes.isSuccess) {
                    val cUser = creatorRes.getOrNull()!!
                    val isCreatorCurator = AppRole.fromServerName(cUser.role) == AppRole.CURATOR

                    val groupInfo = if (isCreatorCurator) {
                        "Куратор Студенческого совета"
                    } else if (cUser.courseNumber != null && cUser.specialityShortTitle != null && cUser.groupName != null) {
                        "студент группы ${cUser.courseNumber}${cUser.specialityShortTitle}-${cUser.groupName}"
                    } else if (cUser.groupName != null) {
                        "студент группы ${cUser.groupName}"
                    } else {
                        "Создатель мероприятия"
                    }
                    creator = Organizer(cUser.id, cUser.name, cUser.surname, cUser.photo, groupInfo)
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

                _state.value = AvailableEventDetailsState.Success(
                    event = event,
                    roles = filteredRoles,
                    creator = creator,
                    showOrganizerCard = showOrganizerCard,
                    showParticipantCard = showParticipantCard
                )
            } else {
                _state.value = AvailableEventDetailsState.Error("Не удалось загрузить данные мероприятия")
            }
        }
    }
}