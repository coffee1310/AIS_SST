package com.example.ais_sst_mobile.presentation.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CoordinatorHomeScreenModel : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _coordinatorEvents = MutableStateFlow(
        listOf(
            EventUiModel(
                id = 1,
                title = "День открытых дверей",
                dateStr = "28 мая, 12:00",
                venue = "Наша страна, актовый зал"
            ),
            EventUiModel(
                id = 2,
                title = "Конференция по карьере",
                dateStr = "5 июня, 10:00",
                venue = "Авангардная улица, 7"
            )
        )
    )
    val coordinatorEvents = _coordinatorEvents.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}