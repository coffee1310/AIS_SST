package com.example.ais_sst_mobile.presentation.events.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.CreateEventRequestDto
import com.example.ais_sst_mobile.data.network.dto.CreateEventRoleRequestDto
import com.example.ais_sst_mobile.data.network.dto.RoleDto
import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import com.example.ais_sst_mobile.domain.repository.EventsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.ktor.util.encodeBase64
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CreateEventScreenModel(
    private val userRepository: UserRepository,
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredActivists = _searchQuery
        .debounce(300)
        .mapLatest { query ->
            if (query.isBlank()) emptyList()
            else userRepository.getAllUsers(page = 0, size = 20, searchQuery = query).getOrNull()?.content ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedOrganizers = MutableStateFlow<List<UserProfileDto>>(emptyList())
    val selectedOrganizers = _selectedOrganizers.asStateFlow()

    private val _globalRoles = MutableStateFlow<List<RoleDto>>(emptyList())
    val globalRoles = _globalRoles.asStateFlow()

    private val _eventPhotoBase64 = MutableStateFlow<String?>(null)
    val eventPhotoBase64 = _eventPhotoBase64.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _effect = Channel<CreateEventEffect>()
    val effect = _effect.receiveAsFlow()

    init { loadGlobalRoles() }

    private fun loadGlobalRoles() {
        viewModelScope.launch {
            eventsRepository.getGlobalRoles().onSuccess { _globalRoles.value = it }
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun addOrganizer(user: UserProfileDto) {
        val currentList = _selectedOrganizers.value.toMutableList()
        if (!currentList.any { it.id == user.id }) {
            currentList.add(user)
            _selectedOrganizers.value = currentList
        }
        _searchQuery.value = ""
    }
    fun removeOrganizer(user: UserProfileDto) { _selectedOrganizers.value = _selectedOrganizers.value.filter { it.id != user.id } }
    fun updateEventPhoto(photoBytes: ByteArray) { _eventPhotoBase64.value = photoBytes.encodeBase64() }
    fun showError(message: String) { viewModelScope.launch { _effect.send(CreateEventEffect.ShowError(message)) } }

    fun createEvent(
        title: String,
        description: String,
        date: String,
        startTime: String,
        endTime: String,
        venue: String,
        isPublic: Boolean,
        isDraft: Boolean,
        roles: List<RoleUiModel>
    ) {
        if (title.isBlank() || venue.isBlank() || date.length != 8 || startTime.length != 4 || endTime.length != 4) {
            showError("Пожалуйста, корректно заполните все обязательные поля со звездочкой (*)")
            return
        }
        if (_eventPhotoBase64.value == null) {
            showError("Пожалуйста, прикрепите обложку (фото) мероприятия!")
            return
        }

        if (!isValidTime(startTime) || !isValidTime(endTime)) {
            showError("Некорректное время начала или конца мероприятия")
            return
        }

        val customRoles = roles.filter { !it.isOrganizer }

        val unselectedRole = customRoles.find { it.globalRoleId == null }
        if (unselectedRole != null) {
            val roleName = unselectedRole.name.ifBlank { "Новая роль" }
            showError("Пожалуйста, выберите роль «$roleName» из выпадающего списка, чтобы привязать её к сектору!")
            return
        }

        for (role in customRoles) {
            if (role.deadlineDate.length != 8 || !isValidTime(role.deadlineTime) || role.peopleCount.isBlank()) {
                showError("Пожалуйста, корректно заполните дедлайны и количество людей для роли «${role.name}»")
                return
            }
        }

        viewModelScope.launch {
            _isLoading.value = true


            val formattedDate = "${date.substring(4, 8)}-${date.substring(2, 4)}-${date.substring(0, 2)}"
            val formattedStartTime = "${formattedDate}T${startTime.substring(0, 2)}:${startTime.substring(2, 4)}:00"
            val formattedEndTime = "${formattedDate}T${endTime.substring(0, 2)}:${endTime.substring(2, 4)}:00"

            val eventRequest = CreateEventRequestDto(
                title = title,
                description = description,
                photo = "data:image/jpeg;base64,${_eventPhotoBase64.value}",
                dateOfEvent = formattedDate,
                startTime = formattedStartTime,
                endTime = formattedEndTime,
                venue = venue,
                isPublic = isPublic,
                isDraft = isDraft,
                organizerIds = emptyList(),
                referenceToPosition = "",
            )

            eventsRepository.createEvent(eventRequest).fold(
                onSuccess = { createdEvent ->

                    var hasErrors = false

                    for (org in _selectedOrganizers.value) {
                        val orgResult = eventsRepository.addOrganizer(createdEvent.id, org.id)
                        if (orgResult.isFailure) {
                        }
                    }

                    for (role in customRoles) {
                        val roleDeadlineDate = "${role.deadlineDate.substring(4, 8)}-${role.deadlineDate.substring(2, 4)}-${role.deadlineDate.substring(0, 2)}"
                        val roleDeadline = "${roleDeadlineDate}T${role.deadlineTime.substring(0, 2)}:${role.deadlineTime.substring(2, 4)}:00"

                        val roleRequest = CreateEventRoleRequestDto(
                            eventId = createdEvent.id,
                            globalEventRoleId = role.globalRoleId!!,
                            capacity = role.peopleCount.toIntOrNull() ?: 1,
                            reserveCapacity = role.reserveCount.toIntOrNull() ?: 0,
                            deadline = roleDeadline,
                            description = role.tasks
                        )

                        val roleResult = eventsRepository.createEventRole(roleRequest)

                        if (roleResult.isFailure) {
                            hasErrors = true
                            showError("Ошибка при создании роли «${role.name}».")
                            break
                        } else {
                        }
                    }

                    _isLoading.value = false

                    if (!hasErrors) {
                        _effect.send(CreateEventEffect.NavigateBack)
                    }
                },
                onFailure = {
                    _isLoading.value = false
                    showError("Не удалось создать мероприятие. Проверьте подключение к сети.")
                }
            )
        }
    }

    private fun isValidTime(timeStr: String): Boolean {
        if (timeStr.length != 4) return false
        val hours = timeStr.substring(0, 2).toIntOrNull() ?: return false
        val minutes = timeStr.substring(2, 4).toIntOrNull() ?: return false
        return hours in 0..23 && minutes in 0..59
    }
}
sealed interface CreateEventEffect {
    data object NavigateBack : CreateEventEffect
    data class ShowError(val message: String) : CreateEventEffect
}