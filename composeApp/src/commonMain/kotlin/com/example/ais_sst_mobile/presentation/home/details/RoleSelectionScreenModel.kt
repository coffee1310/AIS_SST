package com.example.ais_sst_mobile.presentation.home.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface RoleSelectionEffect {
    data class ShowSuccessAndNavigateBack(val rolesCount: Int) : RoleSelectionEffect
    data class ShowError(val message: String) : RoleSelectionEffect
}

data class RoleSelectionItem(
    val id: String, // "ORG", "PARTICIPANT", или ID конкретной роли
    val title: String,
    val description: String,
    val deadlineText: String?
)

sealed interface RoleSelectionState {
    data object Loading : RoleSelectionState
    data class Success(val eventTitle: String, val items: List<RoleSelectionItem>) : RoleSelectionState
    data class Error(val message: String) : RoleSelectionState
}

class EventRoleSelectionScreenModel(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RoleSelectionState>(RoleSelectionState.Loading)
    val state: StateFlow<RoleSelectionState> = _state.asStateFlow()

    // Храним множество выбранных ролей для мульти-выбора
    private val _selectedRoleIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedRoleIds: StateFlow<Set<String>> = _selectedRoleIds.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _effect = Channel<RoleSelectionEffect>()
    val effect = _effect.receiveAsFlow()

    fun loadData(eventId: Int) {
        viewModelScope.launch {
            _state.value = RoleSelectionState.Loading

            val eventRes = eventsRepository.getEventById(eventId)
            val rolesRes = eventsRepository.getEventRoles(eventId)

            if (eventRes.isSuccess && rolesRes.isSuccess) {
                val event = eventRes.getOrNull()!!
                val allRoles = rolesRes.getOrNull()!!

                val items = mutableListOf<RoleSelectionItem>()

                // 1. Карточка "Организатор" (если есть места)
                if (event.maxOrganizersCount > event.currentOrganizersCount) {
                    items.add(
                        RoleSelectionItem(
                            id = "ORG",
                            title = "Организатор",
                            description = "Координация всех этапов мероприятия. Связь с участниками и волонтерами. Решение организационных вопросов",
                            deadlineText = null
                        )
                    )
                }

                // 2. Карточка "Участник" (Свободное И (Публичное ИЛИ мой сектор))
                if (event.isFreeEvent && (event.isPublic || event.isMySector)) {
                    items.add(
                        RoleSelectionItem(
                            id = "PARTICIPANT",
                            title = "Участник",
                            description = "Посещение мероприятия и участие во всех активностях без дополнительных обязанностей",
                            deadlineText = null
                        )
                    )
                }

                // 3. Кастомные роли (та же логика, что в деталях)
                val filteredRoles = if (event.isPublic) {
                    allRoles
                } else {
                    allRoles.filter { it.isMySector == true }
                }

                filteredRoles.forEach { role ->
                    // Форматируем дедлайн
                    val deadlineFormatted = try {
                        if (role.deadline != null) {
                            val parts = role.deadline.substringBefore("T").split("-")
                            if (parts.size == 3) {
                                val day = parts[2].toInt().toString()
                                val month = when (parts[1]) {
                                    "01" -> "янв"; "02" -> "фев"; "03" -> "мар"; "04" -> "апр"
                                    "05" -> "мая"; "06" -> "июн"; "07" -> "июл"; "08" -> "авг"
                                    "09" -> "сен"; "10" -> "окт"; "11" -> "ноя"; "12" -> "дек"
                                    else -> ""
                                }
                                "$day $month"
                            } else null
                        } else null
                    } catch (e: Exception) { null }

                    val desc = if (!role.description.isNullOrBlank()) role.description.trim() else "Описание отсутствует"

                    items.add(
                        RoleSelectionItem(
                            id = role.id.toString(),
                            title = role.globalEventRoleTitle,
                            description = desc,
                            deadlineText = deadlineFormatted
                        )
                    )
                }

                _state.value = RoleSelectionState.Success(event.title, items)

            } else {
                _state.value = RoleSelectionState.Error("Не удалось загрузить роли. Проверьте интернет.")
            }
        }
    }

    fun toggleRole(id: String) {
        val currentSet = _selectedRoleIds.value.toMutableSet()
        if (currentSet.contains(id)) {
            currentSet.remove(id)
        } else {
            currentSet.add(id)
        }
        _selectedRoleIds.value = currentSet
    }

    fun submitApplications(eventId: Int) {
        val selected = _selectedRoleIds.value
        if (selected.isEmpty()) {
            viewModelScope.launch {
                _effect.send(RoleSelectionEffect.ShowError("Пожалуйста, выберите хотя бы одну роль"))
            }
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true

            var successCount = 0
            val failedRoles = mutableListOf<String>()

            for (id in selected) {
                val result = when (id) {
                    "ORG" -> eventsRepository.createOrganizerApplication(eventId)
                    "PARTICIPANT" -> eventsRepository.joinEventAsParticipant(eventId)
                    else -> eventsRepository.applyForEventRole(id.toInt(), "")
                }

                if (result.isSuccess) {
                    successCount++
                } else {
                    val errorMsg = result.exceptionOrNull()?.message.orEmpty().lowercase()

                    // 409 = уже подано → считаем успехом
                    if (errorMsg.contains("409") || errorMsg.contains("уже подана")) {
                        successCount++
                    } else {
                        val title = when (id) {
                            "ORG" -> "Организатор"
                            "PARTICIPANT" -> "Участник"
                            else -> "Роль"
                        }
                        failedRoles.add(title)
                    }
                }
            }

            _isSubmitting.value = false

            when {
                successCount > 0 && failedRoles.isEmpty() -> {
                    _effect.send(RoleSelectionEffect.ShowSuccessAndNavigateBack(successCount))
                }
                successCount > 0 -> {
                    _effect.send(
                        RoleSelectionEffect.ShowError(
                            "Частично отправлено. Не удалось подать: ${failedRoles.joinToString()}"
                        )
                    )
                }
                else -> {
                    _effect.send(RoleSelectionEffect.ShowError("Не удалось отправить заявки"))
                }
            }
        }
    }
}