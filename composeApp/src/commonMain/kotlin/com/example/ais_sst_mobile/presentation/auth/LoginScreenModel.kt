package com.example.ais_sst_mobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.network.dto.AuthResponse
import com.example.ais_sst_mobile.data.network.dto.LoginRequest
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginScreenModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    sealed class State {
        object Initial : State()
        object Loading : State()
        data class Success(val user: AuthResponse) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Initial)
    val state = _state.asStateFlow()

    private var failedAttempts = 0

    private val _showCaptchaDialog = MutableStateFlow(false)
    val showCaptchaDialog = _showCaptchaDialog.asStateFlow()

    private val _currentCaptcha = MutableStateFlow<String?>(null)
    val currentCaptcha = _currentCaptcha.asStateFlow()

    private val _captchaError = MutableStateFlow<String?>(null)
    val captchaError = _captchaError.asStateFlow()

    fun refreshCaptcha(clearError: Boolean = true) {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
        _currentCaptcha.value = (1..6).map { chars.random() }.joinToString("")
        if (clearError) {
            _captchaError.value = null
        }
    }

    fun clearCaptchaError() {
        _captchaError.value = null
    }

    fun verifyCaptcha(input: String) {
        if (input.isBlank()) {
            _captchaError.value = "Введите код"
            return
        }
        if (input != _currentCaptcha.value) {
            refreshCaptcha(clearError = true)
            _captchaError.value = "Неверный код"
            return
        }

        _showCaptchaDialog.value = false
        failedAttempts = 0
    }

    fun login(loginId: String, domain: String, pass: String) {
        if (loginId.isBlank() || pass.isBlank()) {
            _state.value = State.Error("Пожалуйста, заполните все поля")
            return
        }

        if (loginId.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }) {
            _state.value = State.Error("Логин не должен содержать русские буквы")
            return
        }

        if (domain == "@edu.fa.ru" && (loginId.length != 6 || !loginId.all { it.isDigit() })) {
            _state.value = State.Error("Номер студенческого должен состоять из 6 цифр")
            return
        }

        val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}\$")
        if (!passwordRegex.matches(pass)) {
            _state.value = State.Error("Пароль: от 8 символов (буквы, цифры, спецсимволы)")
            return
        }

        if (failedAttempts >= 3) {
            refreshCaptcha()
            _showCaptchaDialog.value = true
            return
        }

        performLoginRequest(loginId, domain, pass)
    }

    private fun performLoginRequest(loginId: String, domain: String, pass: String) {
        _state.value = State.Loading

        viewModelScope.launch {
            try {
                val email = "$loginId$domain"
                val request = LoginRequest(email = email, password = pass)

                val result = authRepository.login(request)

                result.onSuccess { response ->
                    failedAttempts = 0

                    sessionManager.saveAuthToken(response.token)
                    sessionManager.saveUserId(response.id)

                    val roleString = response.roles.firstOrNull()
                    val actualRole = AppRole.fromServerName(roleString)
                    sessionManager.saveRealRole(actualRole)

                    _state.value = State.Success(response)
                }.onFailure { exception ->
                    failedAttempts++

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

                    if (failedAttempts >= 3) {
                        refreshCaptcha()
                        _showCaptchaDialog.value = true
                    }
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