package com.example.ais_sst_mobile.presentation.sectors.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.data.network.dto.UserProfileDto
import com.example.ais_sst_mobile.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.ktor.util.encodeBase64

class CreateSectorScreenModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _allActivists = MutableStateFlow<List<UserProfileDto>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredActivists = combine(_allActivists, _searchQuery) { activists, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            activists.filter {
                it.fullName.contains(query, ignoreCase = true)
            }.take(5) // Показываем топ-5 совпадений
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedCoordinators = MutableStateFlow<List<UserProfileDto>>(emptyList())
    val selectedCoordinators = _selectedCoordinators.asStateFlow()
    private val _sectorPhotoBase64 = MutableStateFlow<String?>(null)
    val sectorPhotoBase64 = _sectorPhotoBase64.asStateFlow()

    init {
        loadActivists()
    }

    fun updateSectorPhoto(photoBytes: ByteArray) {
        _sectorPhotoBase64.value = photoBytes.encodeBase64()
    }
    private fun loadActivists() {
        viewModelScope.launch {
            userRepository.getActivists(page = 0, size = 100)
                .onSuccess { response ->
                    val allLoadedActivists = response.content.toMutableList()
                    val totalPages = response.totalPages

                    if (totalPages > 1) {
                        val deferredPages = (1 until totalPages).map { pageNumber ->
                            async {
                                userRepository.getActivists(page = pageNumber, size = 100)
                                    .getOrNull()?.content ?: emptyList()
                            }
                        }

                        val restActivists = deferredPages.awaitAll().flatten()
                        allLoadedActivists.addAll(restActivists)
                    }

                    _allActivists.value = allLoadedActivists
                }
                .onFailure {
                    it.printStackTrace()
                }
        }
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
}