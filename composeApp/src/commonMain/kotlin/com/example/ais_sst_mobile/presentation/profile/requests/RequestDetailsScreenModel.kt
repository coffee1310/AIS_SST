package com.example.ais_sst_mobile.presentation.profile.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.AccountRequestDto
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface RequestDetailsState {
    data object Loading : RequestDetailsState
    data class Success(val request: AccountRequestDto) : RequestDetailsState
    data class Error(val message: String) : RequestDetailsState
}

class RequestDetailsScreenModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RequestDetailsState>(RequestDetailsState.Loading)
    val state = _state.asStateFlow()

    private val _effect = Channel<String>()
    val effect = _effect.receiveAsFlow()

    private val _isActionComplete = MutableStateFlow(false)
    val isActionComplete = _isActionComplete.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading = _isActionLoading.asStateFlow()

    fun loadRequest(id: Int) {
        viewModelScope.launch {
            _state.value = RequestDetailsState.Loading
            userRepository.getAccountRequestById(id)
                .onSuccess { request ->
                    _state.value = RequestDetailsState.Success(request)
                }
                .onFailure {
                    _state.value = RequestDetailsState.Error("Не удалось загрузить данные заявки")
                }
        }
    }

    fun acceptRequest(id: Int) {
        viewModelScope.launch {
            _isActionLoading.value = true
            userRepository.acceptAccountRequest(id)
                .onSuccess {
                    _effect.send("Заявка успешно принята")
                    _isActionComplete.value = true
                }
                .onFailure {
                    _effect.send("Ошибка: не удалось принять заявку")
                }
            _isActionLoading.value = false
        }
    }

    fun rejectRequest(id: Int, reason: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            userRepository.rejectAccountRequest(id, reason)
                .onSuccess {
                    _effect.send("Заявка отклонена")
                    _isActionComplete.value = true
                }
                .onFailure {
                    _effect.send("Ошибка: не удалось отклонить заявку")
                }
            _isActionLoading.value = false
        }
    }
}