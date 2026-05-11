package com.example.ais_sst_mobile.presentation.profile.activist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.domain.model.User
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ActivistProfileState {
    data object Loading : ActivistProfileState
    data class Success(val user: User) : ActivistProfileState
    data class Error(val message: String) : ActivistProfileState
}

class ActivistProfileScreenModel(
    private val userRepository: UserRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val activeRole = sessionManager.activeRoleFlow
    private val _state = MutableStateFlow<ActivistProfileState>(ActivistProfileState.Loading)
    val state = _state.asStateFlow()

    fun loadActivist(userId: Int) {
        viewModelScope.launch {
            _state.value = ActivistProfileState.Loading
            userRepository.getUserProfileById(userId)
                .onSuccess { userDto ->
                    _state.value = ActivistProfileState.Success(userDto.toDomain())
                }
                .onFailure {
                    it.printStackTrace()
                    _state.value = ActivistProfileState.Error("Не удалось загрузить данные пользователя")
                }
        }
    }
}