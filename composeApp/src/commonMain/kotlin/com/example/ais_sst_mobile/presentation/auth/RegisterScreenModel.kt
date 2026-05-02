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

data class RegisterState(
    val specialities: List<Speciality> = emptyList(),
    val socialStatuses: List<SocialStatus> = emptyList(),
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val registerSuccess: Boolean = false,
    val registerError: String? = null
)

class RegisterScreenModel(
    private val dictionaryRepository: DictionaryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

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

    fun register(
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
            _state.update { it.copy(isLoading = true, registerError = null) }

            try {
                val formattedDate = "${birthDate.substring(4, 8)}-${birthDate.substring(2, 4)}-${birthDate.substring(0, 2)}"
                val fullVkLink = "https://vk.com/${vkLink.trim()}"
                val base64Photo = "data:image/jpeg;base64,${photoBytes.encodeBase64()}"

                val request = RegisterRequest(
                    name = name.trim(),
                    surname = surname.trim(),
                    patronymic = patronymic.trim(),
                    gender = gender,
                    dateOfBirth = formattedDate,
                    courseNumber = course.toInt(),
                    specialityId = specialtyId,
                    groupId = groupId,
                    studentIdNumber = corpEmail.toIntOrNull() ?: 0,
                    studentEmail = "$corpEmail$corpDomain",
                    additionalEmail = addEmail.trim().takeIf { it.isNotEmpty() },
                    phoneNumber = "+7$phone",
                    vkLink = fullVkLink,
                    password = pass,
                    socialStatusesId = socialStatusIds.toList(),
                    photo = base64Photo
                )

                authRepository.register(request)
                    .onSuccess {
                        _state.update { it.copy(isLoading = false, registerSuccess = true) }
                    }
                    .onFailure { e ->
                        _state.update { it.copy(isLoading = false, registerError = e.message ?: "Ошибка при отправке заявки") }
                    }

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, registerError = "Ошибка подготовки данных: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(registerError = null) }
    }
}