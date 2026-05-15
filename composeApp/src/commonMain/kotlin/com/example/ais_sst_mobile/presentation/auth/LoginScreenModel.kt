package com.example.ais_sst_mobile.presentation.auth

import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.SerializationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.network.dto.LoginRequest
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val login: String = "",
    val domain: String = "@edu.fa.ru",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isDomainMenuExpanded: Boolean = false
)

class LoginScreenModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    sealed class ScreenState {
        object Initial : ScreenState()
        object Loading : ScreenState()
        data class Error(val message: String) : ScreenState()
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Initial)
    val screenState = _screenState.asStateFlow()

    private val _effect = MutableSharedFlow<String>()
    val effect = _effect.asSharedFlow()

    private var failedAttempts = 0

    private val _showCaptchaDialog = MutableStateFlow(false)
    val showCaptchaDialog = _showCaptchaDialog.asStateFlow()

    private val _currentCaptcha = MutableStateFlow<String?>(null)
    val currentCaptcha = _currentCaptcha.asStateFlow()

    private val _captchaError = MutableStateFlow<String?>(null)
    val captchaError = _captchaError.asStateFlow()

    fun updateLogin(newValue: String) {
        val domain = _uiState.value.domain
        if (domain == "@edu.fa.ru") {
            if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                _uiState.update { it.copy(login = newValue) }
                resetState()
            }
        } else {
            if (!newValue.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }) {
                _uiState.update { it.copy(login = newValue) }
                resetState()
            }
        }
    }

    fun updatePassword(newValue: String) {
        _uiState.update { it.copy(password = newValue) }
        resetState()
    }

    fun selectDomain(newDomain: String) {
        _uiState.update { currentState ->
            var updatedLogin = currentState.login
            if (newDomain == "@edu.fa.ru" && !updatedLogin.all { it.isDigit() }) {
                updatedLogin = updatedLogin.filter { it.isDigit() }.take(6)
            }
            currentState.copy(
                domain = newDomain,
                login = updatedLogin,
                isDomainMenuExpanded = false
            )
        }
        resetState()
    }

    fun toggleDomainMenu(isExpanded: Boolean) {
        _uiState.update { it.copy(isDomainMenuExpanded = isExpanded) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }


    fun refreshCaptcha(clearError: Boolean = true) {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
        _currentCaptcha.value = (1..6).map { chars.random() }.joinToString("")
        if (clearError) clearCaptchaError()
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


    fun login() {
        val currentUiState = _uiState.value

        val loginId = currentUiState.login.filterNot { it.isWhitespace() }
        val domain = currentUiState.domain
        val pass = currentUiState.password.filterNot { it.isWhitespace() }

        if (loginId.isBlank() || pass.isBlank()) {
            _screenState.value = ScreenState.Error("Пожалуйста, заполните все поля")
            return
        }

        if (domain == "@edu.fa.ru" && loginId.length != 6) {
            _screenState.value = ScreenState.Error("Номер студенческого должен состоять из 6 цифр")
            return
        }

        val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$")
        if (!passwordRegex.matches(pass)) {
            _screenState.value = ScreenState.Error("Пароль: от 8 символов (A-Z, a-z, цифры, спецсимволы)")
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
        _screenState.value = ScreenState.Loading

        viewModelScope.launch {
            try {
                val email = "$loginId$domain"
                val request = LoginRequest(email = email, password = pass)

                authRepository.login(request)
                    .onSuccess { response ->
                        failedAttempts = 0

                        sessionManager.saveAuthToken(response.token)
                        sessionManager.saveRefreshToken(response.refreshToken)
                        sessionManager.saveUserId(response.id)

                        val roleString = response.roles.firstOrNull()
                        val actualRole = AppRole.fromServerName(roleString)
                        sessionManager.saveRealRole(actualRole)

                        _screenState.value = ScreenState.Initial
                        _effect.emit("SUCCESS")
                    }
                    .onFailure { exception ->
                        failedAttempts++

                        val humanMessage = when (exception) {
                            is IOException, is HttpRequestTimeoutException ->
                                "Нет связи с сервером. Проверьте подключение к интернету"

                            is ClientRequestException -> {
                                when (exception.response.status.value) {
                                    400, 401, 403, 404 -> "Неверный логин или пароль"
                                    else -> "Ошибка клиента: ${exception.response.status.value}"
                                }
                            }

                            is JsonConvertException, is SerializationException ->
                                "Неверный логин или пароль"

                            is ServerResponseException ->
                                "Сервер временно недоступен. Попробуйте позже"

                            else -> "Что-то пошло не так. Попробуйте позже"
                        }

                        _screenState.value = ScreenState.Error(humanMessage)

                        if (failedAttempts >= 3) {
                            refreshCaptcha()
                            _showCaptchaDialog.value = true
                        }
                    }
            } catch (e: Exception) {
                _screenState.value = ScreenState.Error("Внутренняя ошибка приложения")
            }
        }
    }

    fun resetState() {
        if (_screenState.value is ScreenState.Error) {
            _screenState.value = ScreenState.Initial
        }
    }
}