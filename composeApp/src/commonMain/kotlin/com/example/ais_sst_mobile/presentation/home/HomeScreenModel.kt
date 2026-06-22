package com.example.ais_sst_mobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.async
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
    private val userRepository: UserRepository, // Добавили репозиторий пользователя
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

            val upcomingDeferred = async {
                eventsRepository.getUpcomingEvents(today.toString(), farFuture.toString())
            }

            // Запрашиваем профиль, чтобы узнать сектора
            val profileDeferred = async {
                userRepository.getUserProfile()
            }

            val upcomingRes = upcomingDeferred.await()
            val profileRes = profileDeferred.await()

            // Приводим сектора к безопасному виду прямо здесь (без пробелов, нижний регистр)
            val userSectorsSafe = profileRes.getOrNull()?.userSectors?.map { it.trim().lowercase() } ?: emptyList()

            // Передаем сектора в функцию!
            val availableDeferred = async {
                eventsRepository.getAvailableEvents(userSectorsSafe)
            }
            val availableRes = availableDeferred.await()

            if (upcomingRes.isSuccess && availableRes.isSuccess) {
                val upcoming = upcomingRes.getOrDefault(emptyList()).take(1)
                val available = availableRes.getOrDefault(emptyList())

                _state.value = ActivistHomeState.Success(
                    upcoming = upcoming,
                    available = available
                )
            } else {
                _state.value = ActivistHomeState.Error("Не удалось загрузить мероприятия. Проверьте подключение к сети.")
            }
        }
    }
}