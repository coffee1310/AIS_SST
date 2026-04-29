package com.example.ais_sst_mobile.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileState {
    data object Loading : ProfileState
    data class Success(val profile: UserUiModel) : ProfileState
    data class Error(val message: String) : ProfileState
}

data class UserUiModel(
    val fullName: String,
    val role: String,
    val eventsCount: String,
    val pointsCount: String,
    val rank: String,
    val photoUrl: String?
)

class ProfileScreenModel : ViewModel() {

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            try {
                // TODO: Здесь будет вызов твоего репозитория: userRepository.getProfile()
                delay(500)
                val mockResponse = UserProfileDto(
                    id = 5,
                    name = "Юлия",
                    surname = "Иванова",
                    patronymic = "Ивановна",
                    events_count = null,
                    points_count = null,
                    rank = 0,
                    dateOfBirth = "2000-01-01",
                    courseNumber = 4,
                    specialityTitle = "Информационные системы",
                    groupTitle = "122",
                    studentEmail = "112233@edu.fa.ru",
                    phoneNumber = "+79999994563",
                    roleTitle = "Activist"
                )

                _state.value = ProfileState.Success(mapDtoToUi(mockResponse))
            } catch (e: Exception) {
                _state.value = ProfileState.Error("Не удалось загрузить профиль")
            }
        }
    }

    private fun mapDtoToUi(dto: UserProfileDto): UserUiModel {
        val fullName = listOfNotNull(dto.surname, dto.name, dto.patronymic)
            .joinToString(" ")
            .trim()

        val roleHumanReadable = when (dto.roleTitle) {
            "Activist" -> "Активист студсовета"
            "Admin" -> "Администратор"
            "Moderator" -> "Модератор"
            else -> "Студент"
        }

        return UserUiModel(
            fullName = fullName,
            role = roleHumanReadable,
            eventsCount = dto.events_count?.toString() ?: "0",
            pointsCount = dto.points_count?.toString() ?: "0",
            rank = if (dto.rank != null && dto.rank > 0) dto.rank.toString() else "-",
            photoUrl = dto.photo
        )
    }

    fun logout() {
        // TODO: Очистить SessionManager и перебросить на экран Login
    }
}