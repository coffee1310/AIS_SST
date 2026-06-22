package com.example.ais_sst_mobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ChairmanHomeEffect {
    data class ShowSnackbar(val message: String) : ChairmanHomeEffect
}

sealed interface ChairmanEventsState {
    data object Loading : ChairmanEventsState
    data class Success(val events: List<Event>) : ChairmanEventsState
    data class Error(val message: String) : ChairmanEventsState
}

class ChairmanHomeScreenModel(
    private val eventsRepository: EventsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _state = MutableStateFlow<ChairmanEventsState>(ChairmanEventsState.Loading)
    val state: StateFlow<ChairmanEventsState> = _state.asStateFlow()

    private val _effect = Channel<ChairmanHomeEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadDashboard()

        if (eventsRepository.getAndClearDeletedEventSignal()) {
            viewModelScope.launch {
                _effect.send(ChairmanHomeEffect.ShowSnackbar("Мероприятие успешно удалено"))
            }
        }
    }

    fun selectTab(index: Int) { _selectedTab.value = index }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.value = ChairmanEventsState.Loading

            // Берем ID из сессии, а не через UserRepository
            val userId = sessionManager.fetchUserId()

            if (userId != null) {
                eventsRepository.getChairmanDashboardEvents(userId)
                    .onSuccess { allEvents ->
                        val overdue = allEvents.filter { it.isOverdue }.sortedBy { it.rawDate }
                        val upcoming = allEvents.filter { !it.isOverdue }.sortedBy { it.rawDate }
                        _state.value = ChairmanEventsState.Success(overdue + upcoming)
                    }
                    .onFailure { error ->
                        _state.value = ChairmanEventsState.Error(error.message ?: "Ошибка")
                    }
            } else {
                _state.value = ChairmanEventsState.Error("Пользователь не авторизован")
            }
        }
    }
}