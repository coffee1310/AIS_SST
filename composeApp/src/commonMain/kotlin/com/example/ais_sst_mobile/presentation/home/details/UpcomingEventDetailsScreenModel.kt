package com.example.ais_sst_mobile.presentation.home.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EventDetailsState {
    data object Loading : EventDetailsState
    data class Success(val event: Event) : EventDetailsState
    data class Error(val message: String) : EventDetailsState
}

class UpcomingEventDetailsScreenModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<EventDetailsState>(EventDetailsState.Loading)
    val state = _state.asStateFlow()

    fun loadEvent(id: Int) {
        viewModelScope.launch {
            _state.value = EventDetailsState.Loading

            eventsRepository.getEventById(id)
                .onSuccess { loadedEvent ->
                    _state.value = EventDetailsState.Success(loadedEvent)
                }
                .onFailure { error ->
                    _state.value = EventDetailsState.Error(error.message ?: "Ошибка загрузки данных")
                }
        }
    }
}