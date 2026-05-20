package com.example.ais_sst_mobile.presentation.home.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.EventRoleDto
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ПЕРЕИМЕНОВАЛИ ИНТЕРФЕЙС, ЧТОБЫ НЕ БЫЛО КОНФЛИКТА
sealed interface CoordinatorEventDetailsState {
    data object Loading : CoordinatorEventDetailsState
    data class Success(val event: Event, val roles: List<EventRoleDto>) : CoordinatorEventDetailsState
    data class Error(val message: String) : CoordinatorEventDetailsState
}

class EventDetailsScreenModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CoordinatorEventDetailsState>(CoordinatorEventDetailsState.Loading)
    val state: StateFlow<CoordinatorEventDetailsState> = _state.asStateFlow()

    fun loadEvent(eventId: Int) {
        viewModelScope.launch {
            _state.value = CoordinatorEventDetailsState.Loading

            val eventResult = eventsRepository.getEventById(eventId)
            val rolesResult = eventsRepository.getEventRoles(eventId)

            if (eventResult.isSuccess && rolesResult.isSuccess) {
                _state.value = CoordinatorEventDetailsState.Success(
                    event = eventResult.getOrNull()!!,
                    roles = rolesResult.getOrNull()!!
                )
            } else {
                _state.value = CoordinatorEventDetailsState.Error("Не удалось загрузить данные мероприятия")
            }
        }
    }
}