package com.example.ais_sst_mobile.presentation.sectors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.core.prefs.SessionManager
import com.example.ais_sst_mobile.data.network.dto.ParticipantDto
import com.example.ais_sst_mobile.data.network.dto.SectorDto
import com.example.ais_sst_mobile.data.network.dto.SectorRequestDto
import com.example.ais_sst_mobile.domain.repository.SectorsRepository
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

sealed interface SectorsState {
    data object Loading : SectorsState
    data class Success(val sectors: List<SectorDto>) : SectorsState
    data class Error(val message: String) : SectorsState
}

class SectorsScreenModel(
    private val repository: SectorsRepository,
    private val userRepository: UserRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val activeRole = sessionManager.activeRoleFlow
    private val _state = MutableStateFlow<SectorsState>(SectorsState.Loading)
    val state = _state.asStateFlow()
    private val _selectedDashboardTab = MutableStateFlow(0)
    val selectedDashboardTab = _selectedDashboardTab.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _participantsState = MutableStateFlow<List<ParticipantDto>>(emptyList())
    val participantsState = _participantsState.asStateFlow()
    private val _isParticipantsLoading = MutableStateFlow(true)
    val isParticipantsLoading = _isParticipantsLoading.asStateFlow()
    private val _requestsState = MutableStateFlow<List<SectorRequestDto>>(emptyList())
    val requestsState = _requestsState.asStateFlow()
    private val _effect = MutableSharedFlow<String>()
    val effect = _effect.asSharedFlow()

    private var currentSectorId: Int? = null
    private val _isRequestsLoading = MutableStateFlow(true)
    val isRequestsLoading = _isRequestsLoading.asStateFlow()

    fun loadRequests() {
        viewModelScope.launch {
            _isRequestsLoading.value = true
            repository.getSectorRequests()
                .onSuccess { rawRequests ->
                    val enrichedRequests = rawRequests.map { request ->
                        async {
                            userRepository.getUserProfileById(request.user_id).fold(
                                onSuccess = { user ->
                                    request.copy(
                                        name = user.name,
                                        surname = user.surname,
                                        patronymic = user.patronymic,
                                        photo = user.photo,
                                        courseNumber = user.courseNumber,
                                        specialityName = user.specialityName,
                                        groupName = user.groupName
                                    )
                                },
                                onFailure = {
                                    request
                                }
                            )
                        }
                    }.awaitAll()

                    _requestsState.value = enrichedRequests
                    _isRequestsLoading.value = false
                }
                .onFailure {
                    it.printStackTrace()
                    _isRequestsLoading.value = false
                }
        }
    }

    fun acceptRequest(requestId: Int) {
        viewModelScope.launch {
            repository.acceptSectorRequest(requestId)
                .onSuccess { message ->
                    _effect.emit(message)
                    loadRequests()
                    currentSectorId?.let { loadParticipants(it) }
                }
                .onFailure {
                    _effect.emit("Ошибка при принятии заявки")
                }
        }
    }
    fun rejectRequest(requestId: Int) {
        viewModelScope.launch {
            repository.rejectSectorRequest(requestId)
                .onSuccess { message ->
                    _effect.emit(message)
                    loadRequests()
                }
                .onFailure {
                    _effect.emit("Ошибка при отклонении заявки")
                }
        }
    }
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
    fun loadParticipants(sectorId: Int) {
        currentSectorId = sectorId
        viewModelScope.launch {
            _isParticipantsLoading.value = true
            repository.getSectorParticipants(sectorId)
                .onSuccess { response ->
                    _participantsState.value = response.content
                    _isParticipantsLoading.value = false
                }
                .onFailure {
                    it.printStackTrace()
                    _isParticipantsLoading.value = false
                }
        }
    }
    fun selectDashboardTab(index: Int) {
        _selectedDashboardTab.value = index
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}