package com.example.ais_sst_mobile.presentation.profile.event_roles.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.CreateRoleRequestDto
import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface EditRoleEffect {
    data object NavigateBack : EditRoleEffect
    class ShowError(val message: String) : EditRoleEffect
}

class EditRoleScreenModel(
    private val dictionaryRepository: DictionaryRepository,
    private val sectorsRepository: SectorsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isScreenLoading = MutableStateFlow(true)
    val isScreenLoading = _isScreenLoading.asStateFlow()

    private val _sectors = MutableStateFlow<List<SectorDto>>(emptyList())
    val sectors = _sectors.asStateFlow()

    // Поля данных роли
    val title = MutableStateFlow("")
    val description = MutableStateFlow("")
    val selectedSector = MutableStateFlow<SectorDto?>(null)

    private val _effect = Channel<EditRoleEffect>()
    val effect = _effect.receiveAsFlow()

    fun loadRoleData(roleId: Int) {
        viewModelScope.launch {
            _isScreenLoading.value = true

            // 1. Сначала загружаем список секторов для выпадающего списка
            sectorsRepository.getSectors()
                .onSuccess { sectorList ->
                    _sectors.value = sectorList

                    // 2. Затем подтягиваем саму роль
                    dictionaryRepository.getEventRoleById(roleId)
                        .onSuccess { role ->
                            title.value = role.title
                            description.value = role.description ?: ""
                            selectedSector.value = sectorList.firstOrNull { it.id == role.sectorId }
                            _isScreenLoading.value = false
                        }
                        .onFailure {
                            showError("Не удалось загрузить данные роли")
                            _isScreenLoading.value = false
                        }
                }
                .onFailure {
                    showError("Не удалось загрузить список секторов")
                    _isScreenLoading.value = false
                }
        }
    }

    fun saveChanges(roleId: Int) {
        val currentTitle = title.value
        val currentDescription = description.value
        val currentSector = selectedSector.value

        if (currentTitle.isBlank()) {
            showError("Название роли обязательно!")
            return
        }
        if (currentSector == null) {
            showError("Выберите сектор!")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val request = CreateRoleRequestDto(
                title = currentTitle.trim(),
                description = currentDescription.trim(),
                sectorId = currentSector.id,
                isDefaultRole = true
            )

            dictionaryRepository.updateEventRole(roleId, request)
                .onSuccess {
                    _isLoading.value = false
                    _effect.send(EditRoleEffect.NavigateBack)
                }
                .onFailure {
                    _isLoading.value = false
                    showError("Ошибка сервера при обновлении роли")
                }
        }
    }

    fun showError(message: String) {
        viewModelScope.launch { _effect.send(EditRoleEffect.ShowError(message)) }
    }
}