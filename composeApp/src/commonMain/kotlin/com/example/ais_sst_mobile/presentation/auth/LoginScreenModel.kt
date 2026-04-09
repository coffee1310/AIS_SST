package com.example.ais_sst_mobile.presentation.auth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.ais_sst_mobile.data.network.dto.LoginRequest
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginScreenModel(
    private val repository: AuthRepository
) : StateScreenModel<LoginScreenModel.State>(State.Idle) {

    // Состояния нашего экрана (MVI)
    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data object Success : State()
        data class Error(val message: String) : State()
    }

    fun login(email: String, pass: String) {
        screenModelScope.launch {
            mutableState.value = State.Loading
            val result = repository.login(LoginRequest(email, pass))

            result.fold(
                onSuccess = {
                    mutableState.value = State.Success
                    // Здесь позже добавим навигацию на Главный экран
                },
                onFailure = {
                    mutableState.value = State.Error(it.message ?: "Ошибка авторизации")
                }
            )
        }
    }
}