package com.example.ais_sst_mobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.RegisterRequest
import com.example.ais_sst_mobile.domain.model.Group
import com.example.ais_sst_mobile.domain.model.SocialStatus
import com.example.ais_sst_mobile.domain.model.Speciality
import com.example.ais_sst_mobile.domain.repository.AuthRepository
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import io.ktor.util.encodeBase64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RegistrationStep {
    FORM,           // Заполнение формы
    VERIFY_CODE     // Ввод кода подтверждения
}

data class RegisterState(
    val specialities: List<Speciality> = emptyList(),
    val socialStatuses: List<SocialStatus> = emptyList(),
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val currentStep: RegistrationStep = RegistrationStep.FORM,
    val registerSuccess: Boolean = false,
    val registerError: String? = null,
    val verificationError: String? = null,
    val isCodeSent: Boolean = false
)

class RegisterScreenModel(
    private val dictionaryRepository: DictionaryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    // Временное хранение данных регистрации
    private var pendingRequest: RegisterRequest? = null
    private var pendingEmail: String? = null

    init {
        loadDictionaries()
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val specResult = dictionaryRepository.getSpecialities()
            val statusResult = dictionaryRepository.getSocialStatuses()
            val groupResult = dictionaryRepository.getGroups()

            _state.update { state ->
                state.copy(
                    specialities = specResult.getOrNull() ?: emptyList(),
                    socialStatuses = statusResult.getOrNull() ?: emptyList(),
                    groups = groupResult.getOrNull() ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }

    /**
     * Вызывается при нажатии кнопки "Зарегистрироваться".
     * Сохраняет данные и отправляет код на почту.
     */
    fun startRegistration(
        surname: String,
        name: String,
        patronymic: String,
        birthDate: String,
        socialStatusIds: Set<Int>,
        gender: String,
        course: String,
        specialtyId: Int,
        groupId: Int,
        corpEmail: String,
        corpDomain: String,
        addEmail: String,
        phone: String,
        vkLink: String,
        pass: String,
        photoBytes: ByteArray
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, registerError = null, verificationError = null) }

            try {
                val cleanCorpEmail = corpEmail.filterNot { it.isWhitespace() }
                val cleanPass = pass.filterNot { it.isWhitespace() }
                val cleanPhone = phone.filterNot { it.isWhitespace() }
                val cleanVkLink = vkLink.filterNot { it.isWhitespace() }
                val cleanAddEmail = addEmail.filterNot { it.isWhitespace() }

                val formattedDate = "${birthDate.substring(4, 8)}-${birthDate.substring(2, 4)}-${birthDate.substring(0, 2)}"
                val fullVkLink = "https://vk.ru/$cleanVkLink"
                val base64Photo = "data:image/jpeg;base64,${photoBytes.encodeBase64()}"

                val fullEmail = "$cleanCorpEmail$corpDomain"

                val request = RegisterRequest(
                    name = name.trim(),
                    surname = surname.trim(),
                    patronymic = patronymic.trim(),
                    gender = gender,
                    dateOfBirth = formattedDate,
                    courseNumber = course.toInt(),
                    specialityId = specialtyId,
                    groupId = groupId,
                    studentIdNumber = cleanCorpEmail.toIntOrNull() ?: 0,
                    studentEmail = fullEmail,
                    additionalEmail = cleanAddEmail.takeIf { it.isNotEmpty() },
                    phoneNumber = "+7$cleanPhone",
                    vkLink = fullVkLink,
                    password = cleanPass,
                    socialStatusesId = socialStatusIds.toList(),
                    photo = base64Photo
                )

                // Сохраняем данные для последующего использования
                pendingRequest = request
                pendingEmail = fullEmail

                // Отправляем код подтверждения
                authRepository.sendRegistrationCode(
                    name = name.trim(),
                    surname = surname.trim(),
                    studentEmail = fullEmail
                ).onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentStep = RegistrationStep.VERIFY_CODE,
                            isCodeSent = true
                        )
                    }
                }.onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            registerError = e.message ?: "Не удалось отправить код подтверждения"
                        )
                    }
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, registerError = "Ошибка подготовки данных: ${e.message}")
                }
            }
        }
    }

    /**
     * Проверка введённого кода и создание заявки
     */
    fun verifyCodeAndCreateAccount(code: String) {
        val request = pendingRequest
        val email = pendingEmail

        if (request == null || email == null) {
            _state.update { it.copy(verificationError = "Данные регистрации утеряны. Начните заново.") }
            return
        }

        if (code.isBlank()) {
            _state.update { it.copy(verificationError = "Введите код из письма") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, verificationError = null) }

            authRepository.verifyAndCreateAccount(
                email = email,
                code = code.trim(),
                accountRequest = request
            ).onSuccess {
                _state.update {
                    it.copy(
                        isLoading = false,
                        registerSuccess = true,
                        currentStep = RegistrationStep.FORM
                    )
                }
                // Очищаем временные данные
                pendingRequest = null
                pendingEmail = null
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        verificationError = e.message ?: "Неверный код или ошибка создания заявки"
                    )
                }
            }
        }
    }

    fun goBackToForm() {
        _state.update {
            it.copy(
                currentStep = RegistrationStep.FORM,
                isCodeSent = false,
                verificationError = null
            )
        }
        pendingRequest = null
        pendingEmail = null
    }

    fun clearError() {
        _state.update { it.copy(registerError = null, verificationError = null) }
    }
}