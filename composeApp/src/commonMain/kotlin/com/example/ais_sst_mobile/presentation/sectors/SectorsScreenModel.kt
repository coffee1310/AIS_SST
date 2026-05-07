package com.example.ais_sst_mobile.presentation.sectors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SectorsState {
    data object Loading : SectorsState
    data class Success(val sectors: List<SectorDto>) : SectorsState
    data class Error(val message: String) : SectorsState
}

class SectorsScreenModel(
    private val repository: SectorsRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val activeRole = sessionManager.activeRoleFlow
    private val _state = MutableStateFlow<SectorsState>(SectorsState.Loading)
    val state = _state.asStateFlow()

    init {
        loadSectors()
    }

    private fun loadSectors() {
        viewModelScope.launch {
            _state.value = SectorsState.Loading

            repository.getSectors()
                .onSuccess { sectors ->
                    _state.value = SectorsState.Success(sectors)
                }
                .onFailure {
                    _state.value = SectorsState.Error("Не удалось загрузить список секторов")
                }
        }
    }
}