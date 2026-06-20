package com.example.ais_sst_mobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.domain.model.Event
import com.example.ais_sst_mobile.domain.repository.EventsRepository
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
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val nextWeek = today.plus(DatePeriod(days = 7))

            val upcomingDeferred = async {
                // Сначала ищем в ближайшие 7 дней
                val firstTry = eventsRepository.getUpcomingEvents(today.toString(), nextWeek.toString())

                if (firstTry.isSuccess && firstTry.getOrDefault(emptyList()).isEmpty()) {
                    // Если в ближайшие 7 дней пусто, берем горизонт в 1 год и оставляем только 1 самое ближайшее
                    val farFuture = today.plus(DatePeriod(years = 1))
                    val secondTry = eventsRepository.getUpcomingEvents(today.toString(), farFuture.toString())

                    if (secondTry.isSuccess) {
                        Result.success(secondTry.getOrDefault(emptyList()).take(1))
                    } else {
                        secondTry
                    }
                } else {
                    firstTry
                }
            }

            val availableDeferred = async {
                eventsRepository.getAvailableEvents()
            }

            val upcomingRes = upcomingDeferred.await()
            val availableRes = availableDeferred.await()

            if (upcomingRes.isSuccess && availableRes.isSuccess) {
                _state.value = ActivistHomeState.Success(
                    upcoming = upcomingRes.getOrDefault(emptyList()),
                    available = availableRes.getOrDefault(emptyList())
                )
            } else {
                _state.value = ActivistHomeState.Error("Не удалось загрузить мероприятия. Проверьте подключение к сети.")
            }
        }
    }
}