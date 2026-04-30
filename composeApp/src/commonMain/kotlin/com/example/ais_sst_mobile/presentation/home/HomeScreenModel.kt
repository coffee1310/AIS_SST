package com.example.ais_sst_mobile.presentation.home

import androidx.lifecycle.ViewModel
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.presentation.components.EventUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeScreenModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    val activeRole = sessionManager.activeRoleFlow

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val _upcomingEvents = MutableStateFlow(
        listOf(
            EventUiModel(1, "Встреча с выпускниками", "1 июня, 12:00", "Кронштадтский бульвар, 37Б, Актовый зал"),
            EventUiModel(2, "Студенческая весна", "15 июня, 18:00", "Главный кампус, Концертный зал"),
            EventUiModel(5, "Встреча с выпускниками", "1 июня, 12:00", "Кронштадтский бульвар, 37Б, Актовый зал")
        )
    )
    val upcomingEvents = _upcomingEvents.asStateFlow()

    private val _availableEvents = MutableStateFlow(
        listOf(
            EventUiModel(3, "День открытых дверей", "1 июня, 12:00", "Кронштадтский бульвар, 37Б, Актовый зал"),
            EventUiModel(4, "Конференция по карьере", "5 июня, 10:00", "Авангардная улица, 7"),
            EventUiModel(6, "Конференция по карьере", "5 июня, 10:00", "Авангардная улица, 7")
        )
    )
    val availableEvents = _availableEvents.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }
}