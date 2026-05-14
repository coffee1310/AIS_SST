package com.example.ais_sst_mobile.presentation.profile.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

data class BoardState(
    val isLoading: Boolean = true,
    val chairman: UserProfileDto? = null,
    val deputies: List<UserProfileDto> = emptyList(),
    val secretaries: List<UserProfileDto> = emptyList(),
    val sectors: List<SectorDto> = emptyList(),
    val error: String? = null
)

class BoardScreenModel(
    private val userRepository: UserRepository,
    private val sectorsRepository: SectorsRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val activeRole = sessionManager.activeRoleFlow

    private val _state = MutableStateFlow(BoardState())
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _effect = Channel<String>()
    val effect = _effect.receiveAsFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun appointCoordinator(sectorId: Int, userId: Int) {
        viewModelScope.launch {
            sectorsRepository.appointCoordinator(sectorId, userId)
                .onSuccess {
                    _searchQuery.value = ""
                    loadBoardData()
                    _effect.send("Координатор успешно назначен!")
                }
                .onFailure { error ->
                    error.printStackTrace()
                    _effect.send(error.message ?: "Не удалось назначить координатора")
                }
        }
    }

    init {
        loadBoardData()
    }

    private fun loadBoardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val chairmanDef = async { userRepository.getUsersByRole("Chairman").getOrNull()?.firstOrNull() }
                val deputiesDef = async { userRepository.getUsersByRole("Deputy_chairman").getOrNull() ?: emptyList() }
                val secretariesDef = async { userRepository.getUsersByRole("Secretary").getOrNull() ?: emptyList() }
                val sectorsDef = async { sectorsRepository.getSectors().getOrNull() ?: emptyList() }

                _state.update {
                    it.copy(
                        isLoading = false,
                        chairman = chairmanDef.await(),
                        deputies = deputiesDef.await(),
                        secretaries = secretariesDef.await(),
                        sectors = sectorsDef.await()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isLoading = false, error = "Ошибка загрузки данных") }
            }
        }
    }
    fun removeCoordinator(sectorId: Int, userId: Int) {
        viewModelScope.launch {
            sectorsRepository.removeCoordinator(sectorId, userId)
                .onSuccess {
                    loadBoardData()
                    _effect.send("Координатор успешно исключен")
                }
                .onFailure { error ->
                    error.printStackTrace()
                    _effect.send(error.message ?: "Не удалось удалить координатора")
                }
        }
    }
    fun changeCoordinator(sectorId: Int, oldUserId: Int, newUserId: Int) {
        viewModelScope.launch {
            sectorsRepository.removeCoordinator(sectorId, oldUserId)
                .onSuccess {
                    sectorsRepository.appointCoordinator(sectorId, newUserId)
                        .onSuccess {
                            _searchQuery.value = ""
                            loadBoardData()
                            _effect.send("Координатор успешно изменен!")
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            _effect.send(error.message ?: "Старый удален, но нового назначить не удалось")
                            loadBoardData()
                        }
                }
                .onFailure { error ->
                    error.printStackTrace()
                    _effect.send(error.message ?: "Не удалось удалить текущего координатора")
                }
        }
    }
}