package com.example.ais_sst_mobile.presentation.profile.event_roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.EventGlobalRoleDto
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EventRolesState {
    object Loading : EventRolesState
    data class Success(
        val roles: List<EventGlobalRoleDto>,
        val searchQuery: String = ""
    ) : EventRolesState
    data class Error(val message: String) : EventRolesState
}

class EventRolesScreenModel(
    private val repository: DictionaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow<EventRolesState>(EventRolesState.Loading)
    val state = _state.asStateFlow()

    private var allRoles = emptyList<EventGlobalRoleDto>()

    init {
        loadRoles()
    }

    fun loadRoles() {
        viewModelScope.launch {
            _state.value = EventRolesState.Loading
            repository.getEventRoles()
                .onSuccess { roles ->
                    allRoles = roles
                    _state.value = EventRolesState.Success(roles)
                }
                .onFailure {
                    _state.value = EventRolesState.Error("Не удалось загрузить роли")
                }
        }
    }

    fun search(query: String) {
        val currentState = _state.value
        if (currentState is EventRolesState.Success) {
            val filtered = if (query.isBlank()) {
                allRoles
            } else {
                allRoles.filter { it.title.contains(query, ignoreCase = true) }
            }
            _state.value = currentState.copy(roles = filtered, searchQuery = query)
        }
    }
}