package com.example.ais_sst_mobile.presentation.profile.event_roles.create

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

sealed interface CreateRoleEffect {
    data object NavigateBack : CreateRoleEffect
    data class ShowError(val message: String) : CreateRoleEffect
}

class CreateRoleScreenModel(
    private val dictionaryRepository: DictionaryRepository,
    private val sectorsRepository: SectorsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _sectors = MutableStateFlow<List<SectorDto>>(emptyList())
    val sectors = _sectors.asStateFlow()

    private val _effect = Channel<CreateRoleEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadSectors()
    }

    private fun loadSectors() {
        viewModelScope.launch {
            sectorsRepository.getSectors()
                .onSuccess { _sectors.value = it }
                .onFailure { showError("Не удалось загрузить список секторов") }
        }
    }

    fun createRole(title: String, description: String, sectorId: Int?) {
        if (title.isBlank()) {
            showError("Название роли обязательно!")
            return
        }
        if (sectorId == null) {
            showError("Выберите сектор!")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val request = CreateRoleRequestDto(
                title = title.trim(),
                description = description.trim(),
                sectorId = sectorId,
                isDefaultRole = true
            )

            dictionaryRepository.createEventRole(request)
                .onSuccess {
                    _isLoading.value = false
                    _effect.send(CreateRoleEffect.NavigateBack)
                }
                .onFailure {
                    _isLoading.value = false
                    showError("Ошибка сервера при создании роли")
                }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch { _effect.send(CreateRoleEffect.ShowError(message)) }
    }
}