package com.example.ais_sst_mobile.presentation.profile.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.AccountRequestDto
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RequestsState {
    data object Loading : RequestsState
    data class Success(
        val requests: List<AccountRequestDto>,
        val searchQuery: String = ""
    ) : RequestsState
    data class Error(val message: String) : RequestsState
}

class AccountRequestsScreenModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RequestsState>(RequestsState.Loading)
    val state = _state.asStateFlow()
    private val _effect = Channel<String>()
    val effect = _effect.receiveAsFlow()
    private var allRequests: List<AccountRequestDto> = emptyList()

    init {
        loadRequests()
    }

    private fun loadRequests() {
        viewModelScope.launch {
            _state.value = RequestsState.Loading
            userRepository.getAccountRequests()
                .onSuccess { requests ->
                    allRequests = requests
                    _state.value = RequestsState.Success(requests)
                }
                .onFailure {
                    _state.value = RequestsState.Error("Не удалось загрузить заявки")
                }
        }
    }

    fun search(query: String) {
        val currentState = _state.value
        if (currentState is RequestsState.Success) {
            val filtered = if (query.isBlank()) {
                allRequests
            } else {
                allRequests.filter {
                    "${it.surname} ${it.name} ${it.patronymic}".contains(query, ignoreCase = true)
                }
            }
            _state.update { currentState.copy(requests = filtered, searchQuery = query) }
        }
    }

    fun acceptRequest(id: Int) {
        viewModelScope.launch {
            userRepository.acceptAccountRequest(id)
                .onSuccess {
                    removeRequestFromList(id)
                    _effect.send("Заявка принята")
                }
                .onFailure { exception ->
                    _effect.send("Ошибка: не удалось принять заявку")
                }
        }
    }

    fun rejectRequest(id: Int, reason: String) {
        viewModelScope.launch {
            userRepository.rejectAccountRequest(id, reason)
                .onSuccess {
                    removeRequestFromList(id)
                    _effect.send("Заявка отклонена")
                }
                .onFailure { exception ->
                    _effect.send("Ошибка: не удалось отклонить заявку")
                }
        }
    }

    private fun removeRequestFromList(id: Int) {
        allRequests = allRequests.filter { it.id != id }
        search((_state.value as? RequestsState.Success)?.searchQuery ?: "")
    }
}