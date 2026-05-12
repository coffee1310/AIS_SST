package com.example.ais_sst_mobile.presentation.sectors.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.CreateSectorRequestDto
import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.ktor.util.encodeBase64

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CreateSectorScreenModel(
    private val userRepository: UserRepository,
    private val sectorRepository: SectorsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredActivists = _searchQuery
        .debounce(300)
        .mapLatest { query ->
            if (query.isBlank()) {
                emptyList()
            } else {
                userRepository.getActivists(page = 0, size = 20, searchQuery = query)
                    .getOrNull()?.content ?: emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedCoordinators = MutableStateFlow<List<UserProfileDto>>(emptyList())
    val selectedCoordinators = _selectedCoordinators.asStateFlow()

    private val _sectorPhotoBase64 = MutableStateFlow<String?>(null)
    val sectorPhotoBase64 = _sectorPhotoBase64.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _effect = Channel<CreateSectorEffect>()
    val effect = _effect.receiveAsFlow()

    fun updateSectorPhoto(photoBytes: ByteArray) {
        _sectorPhotoBase64.value = photoBytes.encodeBase64()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addCoordinator(user: UserProfileDto) {
        val currentList = _selectedCoordinators.value.toMutableList()
        if (!currentList.any { it.id == user.id }) {
            currentList.add(user)
            _selectedCoordinators.value = currentList
        }
        _searchQuery.value = ""
    }

    fun removeCoordinator(user: UserProfileDto) {
        _selectedCoordinators.value = _selectedCoordinators.value.filter { it.id != user.id }
    }

    fun createSector(title: String, description: String) {
        if (title.isBlank()) {
            viewModelScope.launch { _effect.send(CreateSectorEffect.ShowError("Название сектора обязательно!")) }
            return
        }
        if (_selectedCoordinators.value.isEmpty()) {
            viewModelScope.launch { _effect.send(CreateSectorEffect.ShowError("Обязательно выберите координатора!")) }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true

            val coordinatorId = _selectedCoordinators.value.firstOrNull()?.id

            val photoString = _sectorPhotoBase64.value?.let { "data:image/jpeg;base64,$it" }

            val request = CreateSectorRequestDto(
                title = title,
                description = description,
                currentCoordinator_id = coordinatorId,
                photo = photoString
            )

            sectorRepository.createSector(request)
                .onSuccess {
                    _isLoading.value = false
                    _effect.send(CreateSectorEffect.NavigateBack) // Успех! Уходим назад
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _effect.send(CreateSectorEffect.ShowError("Ошибка сервера: ${error.message}"))
                }
        }
    }
    fun showError(message: String) {
        viewModelScope.launch {
            _effect.send(CreateSectorEffect.ShowError(message))
        }
    }
}

sealed interface CreateSectorEffect {
    data object NavigateBack : CreateSectorEffect
    data class ShowError(val message: String) : CreateSectorEffect
}