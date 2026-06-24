package com.example.ais_sst_mobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordScreenModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // === Исправленный regex (вынесен из функции) ===
    private val passwordRegex = Regex(
        """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?])[a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]{8,}$"""
    )

    data class UiState(
        val email: String = "",
        val domain: String = "@edu.fa.ru",
        val code: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val step: Int = 1,
        val isSuccess: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun updateDomain(newDomain: String) {
        _uiState.update { it.copy(domain = newDomain, error = null) }
    }

    fun updateCode(value: String) {
        _uiState.update { it.copy(code = value, error = null) }
    }

    fun updateNewPassword(value: String) {
        _uiState.update { it.copy(newPassword = value, error = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun requestCode() {
        val currentState = _uiState.value
        val fullEmail = currentState.email.trim() + currentState.domain

        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(error = "Введите номер студбилета") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.requestPasswordReset(fullEmail)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, step = 2) }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Не удалось отправить код"
                        )
                    }
                }
        }
    }

    fun resetPassword() {
        val currentState = _uiState.value
        val fullEmail = currentState.email.trim() + currentState.domain
        val code = currentState.code.trim()
        val newPass = currentState.newPassword.trim()
        val confirmPass = currentState.confirmPassword.trim()

        if (code.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }

        if (newPass != confirmPass) {
            _uiState.update { it.copy(error = "Пароли не совпадают") }
            return
        }

        // Проверка пароля
        if (!passwordRegex.matches(newPass)) {
            _uiState.update {
                it.copy(error = "Пароль должен содержать минимум 8 символов (A-Z, a-z, цифры и спецсимвол)")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.verifyPasswordReset(fullEmail, code, newPass)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Не удалось сбросить пароль"
                        )
                    }
                }
        }
    }
}