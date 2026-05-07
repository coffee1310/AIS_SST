package com.example.ais_sst_mobile.presentation.sectors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.domain.model.AppRole
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SectorDetailsState {
    data object Loading : SectorDetailsState
    data class Success(val sector: SectorDto, val activeRole: AppRole) : SectorDetailsState
    data class Error(val message: String) : SectorDetailsState
}

class SectorDetailsScreenModel(
    private val repository: SectorsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<SectorDetailsState>(SectorDetailsState.Loading)
    val state = _state.asStateFlow()

    fun loadSector(id: Int) {
        viewModelScope.launch {
            _state.value = SectorDetailsState.Loading
            val activeRole = sessionManager.activeRoleFlow.value

            repository.getSectorById(id)
                .onSuccess { sector ->
                    _state.value = SectorDetailsState.Success(sector, activeRole)
                }
                .onFailure {
                    _state.value = SectorDetailsState.Error("Не удалось загрузить данные сектора")
                }
        }
    }

    fun joinSector(id: Int) {
        viewModelScope.launch {
            repository.joinSector(id).onSuccess {
                loadSector(id)
            }
        }
    }

    fun leaveSector(id: Int) {
        viewModelScope.launch {
            repository.leaveSector(id).onSuccess {
                loadSector(id)
            }
        }
    }
}