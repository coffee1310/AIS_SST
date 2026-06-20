package com.example.ais_sst_mobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface CoordinatorHomeEffect {
    data class ShowSnackbar(val message: String) : CoordinatorHomeEffect
}

sealed interface CoordinatorEventsState {
    data object Loading : CoordinatorEventsState
    data class Success(val events: List<Event>) : CoordinatorEventsState
    data class Error(val message: String) : CoordinatorEventsState
}

class CoordinatorHomeScreenModel(
    private val eventsRepository: EventsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _state = MutableStateFlow<CoordinatorEventsState>(CoordinatorEventsState.Loading)
    val state: StateFlow<CoordinatorEventsState> = _state.asStateFlow()

    private val _effect = Channel<CoordinatorHomeEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadDashboard()

        // Проверяем, было ли только что удалено мероприятие
        if (eventsRepository.getAndClearDeletedEventSignal()) {
            viewModelScope.launch {
                _effect.send(CoordinatorHomeEffect.ShowSnackbar("Мероприятие успешно удалено"))
            }
        }
    }

    fun selectTab(index: Int) { _selectedTab.value = index }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.value = CoordinatorEventsState.Loading

            userRepository.getUserProfile()
                .onSuccess { user ->
                    eventsRepository.getCoordinatorDashboardEvents(user.id)
                        .onSuccess { allEvents ->
                            val overdue = allEvents.filter { it.isOverdue }.sortedBy { it.rawDate }
                            val upcoming = allEvents.filter { !it.isOverdue }.sortedBy { it.rawDate }

                            _state.value = CoordinatorEventsState.Success(overdue + upcoming)
                        }
                        .onFailure { error ->
                            _state.value = CoordinatorEventsState.Error(error.message ?: "Не удалось загрузить мероприятия")
                        }
                }
                .onFailure {
                    _state.value = CoordinatorEventsState.Error("Не удалось получить профиль пользователя")
                }
        }
    }
}