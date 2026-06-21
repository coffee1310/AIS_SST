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
        val creator: Organizer?
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

            if (eventResult.isSuccess && rolesResult.isSuccess) {
                var event = eventResult.getOrNull()!!
                val roles = rolesResult.getOrNull()!!

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
                    roles = roles,
                    creator = creator
                )
            } else {
                _state.value = AvailableEventDetailsState.Error("Не удалось загрузить данные мероприятия")
            }
        }
    }
}