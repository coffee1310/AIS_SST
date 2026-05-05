package com.example.ais_sst_mobile.presentation.profile.my_data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.domain.model.User
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MyDataState {
    data object Loading : MyDataState
    data class Success(val user: User) : MyDataState
    data class Error(val message: String) : MyDataState
}

class MyDataScreenModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MyDataState>(MyDataState.Loading)
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = MyDataState.Loading
            userRepository.getUserProfile()
                .onSuccess { user ->
                    _state.value = MyDataState.Success(user)
                }
                .onFailure {
                    _state.value = MyDataState.Error("Не удалось загрузить данные")
                }
        }
    }
}