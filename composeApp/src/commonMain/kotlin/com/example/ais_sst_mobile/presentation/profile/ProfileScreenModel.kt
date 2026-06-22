package com.example.ais_sst_mobile.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.domain.model.User
import com.example.ais_sst_mobile.domain.repository.UserRepository
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

class ProfileScreenModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val activeRole = sessionManager.activeRoleFlow

    val realRole = sessionManager.getRealRole()

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state = _state.asStateFlow()

    init {

    }

    fun setRole(newRole: AppRole) {
        if (newRole == realRole || newRole == AppRole.ACTIVIST) {
            sessionManager.setActiveRole(newRole)
        }
    }

    public fun loadProfile() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading

            userRepository.getUserProfile()
                .onSuccess { user ->
                    _state.value = ProfileState.Success(mapDomainToUi(user))
                }
                .onFailure { error ->
                    _state.value = ProfileState.Error("Не удалось загрузить профиль")
                }
        }
    }

    private fun mapDomainToUi(user: User): UserUiModel {
        val fullName = listOfNotNull(user.surname, user.name, user.patronymic)
            .joinToString(" ")
            .trim()

        val roleHumanReadable = when (user.roleTitle) {
            "Administrator" -> "Администратор"
            "Secretary" -> "Секретарь студсовета"
            "Chairman" -> "Председатель студсовета"
            "Sector_coordinator" -> "Координатор сектора"
            "Deputy_chairman" -> "Заместитель председателя"
            "Curator" -> "Куратор студсовета"
            "Activist" -> "Активист студсовета"
            else -> user.roleTitle
        }

        return UserUiModel(
            fullName = fullName,
            role = roleHumanReadable,
            eventsCount = user.eventsCount?.toString() ?: "0",
            pointsCount = user.pointsCount?.toString() ?: "0",
            rank = if (user.rank != null && user.rank > 0) user.rank.toString() else "-",
            photoUrl = user.photo
        )
    }

    fun logout() {
        sessionManager.logout()
    }
}