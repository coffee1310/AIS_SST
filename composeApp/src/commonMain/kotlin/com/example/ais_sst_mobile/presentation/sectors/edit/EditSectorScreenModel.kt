package com.example.ais_sst_mobile.presentation.sectors.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.UpdateSectorRequestDto
import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import io.ktor.util.encodeBase64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EditSectorScreenModel(
    private val userRepository: UserRepository,
    private val sectorRepository: SectorsRepository
) : ViewModel() {

    private var currentSectorId: Int = -1

    val title = MutableStateFlow("")
    val description = MutableStateFlow("")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredActivists = _searchQuery
        .debounce(300)
        .mapLatest { query ->
            if (query.isBlank()) emptyList()
            else userRepository.getActivists(page = 0, size = 20, searchQuery = query).getOrNull()?.content ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedCoordinators = MutableStateFlow<List<UserProfileDto>>(emptyList())
    val selectedCoordinators = _selectedCoordinators.asStateFlow()

    private val _sectorPhotoBase64 = MutableStateFlow<String?>(null)
    val sectorPhotoBase64 = _sectorPhotoBase64.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isScreenLoading = MutableStateFlow(true)
    val isScreenLoading = _isScreenLoading.asStateFlow()

    private val _effect = Channel<EditSectorEffect>()
    val effect = _effect.receiveAsFlow()

    fun loadSector(sectorId: Int) {
        currentSectorId = sectorId
        viewModelScope.launch {
            _isScreenLoading.value = true
            sectorRepository.getSectorById(sectorId)
                .onSuccess { sector ->
                    title.value = sector.title
                    description.value = sector.description
                    _sectorPhotoBase64.value = sector.photo

                    _selectedCoordinators.value = sector.coordinators.map { coord ->
                        UserProfileDto(
                            id = coord.studentId,
                            name = coord.studentName,
                            surname = coord.studentSurname,
                            patronymic = coord.studentPatronymic,
                            photo = coord.studentPhoto
                        )
                    }
                    _isScreenLoading.value = false
                }
                .onFailure {
                    _isScreenLoading.value = false
                    _effect.send(EditSectorEffect.ShowError("Не удалось загрузить данные сектора"))
                }
        }
    }

    fun updateTitle(newTitle: String) { title.value = newTitle }
    fun updateDescription(newDesc: String) { description.value = newDesc }
    fun updateSectorPhoto(photoBytes: ByteArray) { _sectorPhotoBase64.value = photoBytes.encodeBase64() }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }

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

    fun saveChanges() {
        if (title.value.isBlank()) {
            showError("Название сектора обязательно!")
            return
        }
        if (_selectedCoordinators.value.isEmpty()) {
            showError("Обязательно выберите хотя бы одного координатора!")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true

            val coordinatorIdsList = _selectedCoordinators.value.map { it.id }

            var photoString = _sectorPhotoBase64.value
            if (photoString != null && !photoString.startsWith("data:image")) {
                photoString = "data:image/jpeg;base64,$photoString"
            }

            val request = UpdateSectorRequestDto(
                title = title.value,
                description = description.value,
                isActive = true,
                photo = photoString,
                coordinatorIds = coordinatorIdsList
            )

            sectorRepository.updateSector(currentSectorId, request)
                .onSuccess {
                    _isLoading.value = false
                    _effect.send(EditSectorEffect.NavigateBack)
                }
                .onFailure { error ->
                    _isLoading.value = false
                    showError(error.message ?: "Не удалось обновить сектор")
                }
        }
    }

    fun showError(message: String) {
        viewModelScope.launch { _effect.send(EditSectorEffect.ShowError(message)) }
    }
}

sealed interface EditSectorEffect {
    data object NavigateBack : EditSectorEffect
    data class ShowError(val message: String) : EditSectorEffect
}