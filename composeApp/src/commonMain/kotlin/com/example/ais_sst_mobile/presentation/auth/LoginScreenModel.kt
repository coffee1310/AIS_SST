package com.example.ais_sst_mobile.presentation.auth

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.ais_sst_mobile.data.network.dto.AuthResponse
import com.example.ais_sst_mobile.data.network.dto.LoginRequest
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginScreenModel(
    private val authRepository: AuthRepository
) : ScreenModel {

    sealed class State {
        object Initial : State()
        object Loading : State()
        data class Success(val user: AuthResponse) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Initial)
    val state = _state.asStateFlow()

    fun login(studentId: String, pass: String) {
        if (studentId.isBlank() || pass.isBlank()) {
            _state.value = State.Error("Пожалуйста, заполните все поля")
            return
        }

        if (studentId.length != 6 || !studentId.all { it.isDigit() }) {
            _state.value = State.Error("Номер студенческого должен состоять из 6 цифр")
            return
        }

        val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9]{8,}\$")
        if (!passwordRegex.matches(pass)) {
            _state.value = State.Error("Пароль от 8 символов: латинский алфавит (заглавные и строчные) и цифры")
            return
        }

        _state.value = State.Loading

        screenModelScope.launch {
            try {
                val email = "$studentId@edu.fa.ru"
                val request = LoginRequest(email = email, password = pass)

                val result = authRepository.login(request)

                result.onSuccess { response ->
                    _state.value = State.Success(response)
                }.onFailure { exception ->
                    val errorString = exception.toString()
                    val message = exception.message ?: ""

                    val humanMessage = when {
                        message.contains("Failed to connect") ||
                                message.contains("Connection refused") ||
                                message.contains("timeout") ->
                            "Нет связи с сервером. Проверьте подключение интернета"

                        message.contains("401") ||
                                message.contains("403") ||
                                message.contains("404") ||
                                errorString.contains("SerializationException") ||
                                errorString.contains("JsonConvertException") ||
                                message.contains("Illegal input") ->
                            "Неверный логин или пароль"

                        else -> "Что-то пошло не так: $message"
                    }

                    _state.value = State.Error(humanMessage)
                }

            } catch (e: Exception) {
                _state.value = State.Error("Внутренняя ошибка приложения")
            }
        }
    }

    fun resetState() {
        if (_state.value is State.Error) {
            _state.value = State.Initial
        }
    }
}