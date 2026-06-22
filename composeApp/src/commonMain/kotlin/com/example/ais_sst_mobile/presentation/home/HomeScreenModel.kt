package com.example.ais_sst_mobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

sealed interface ActivistHomeState {
    data object Loading : ActivistHomeState
    data class Success(val upcoming: List<Event>, val available: List<Event>) : ActivistHomeState
    data class Error(val message: String) : ActivistHomeState
}

class HomeScreenModel(
    private val eventsRepository: EventsRepository,
    private val userRepository: UserRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val activeRole = sessionManager.activeRoleFlow

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _state = MutableStateFlow<ActivistHomeState>(ActivistHomeState.Loading)
    val state: StateFlow<ActivistHomeState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = ActivistHomeState.Loading

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val farFuture = today.plus(DatePeriod(years = 1))

            // 1. Сначала грузим одно
            val upcomingRes = eventsRepository.getUpcomingEvents(today.toString(), farFuture.toString())

            // 2. Ждем, чтобы сервер "выдохнул"
            delay(500)

            // 3. Потом грузим другое
            val availableRes = eventsRepository.getAvailableEvents()

            if (upcomingRes.isSuccess && availableRes.isSuccess) {
                _state.value = ActivistHomeState.Success(
                    upcoming = upcomingRes.getOrDefault(emptyList()).take(1),
                    available = availableRes.getOrDefault(emptyList())
                )
            } else {
                _state.value = ActivistHomeState.Error("Ошибка загрузки")
            }
        }
    }
}