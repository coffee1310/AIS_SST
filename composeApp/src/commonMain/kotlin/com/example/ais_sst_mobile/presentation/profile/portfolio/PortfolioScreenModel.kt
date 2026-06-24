package com.example.ais_sst_mobile.presentation.profile.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PortfolioUiState {
    data object Idle : PortfolioUiState
    data object Loading : PortfolioUiState
    data class Success(val message: String) : PortfolioUiState
    data class Error(val message: String) : PortfolioUiState
}

class PortfolioScreenModel(
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortfolioUiState>(PortfolioUiState.Idle)
    val uiState = _uiState.asStateFlow()

    /**
     * Загрузка портфолио (ZIP или PDF)
     */
    fun uploadPortfolio(fileBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            _uiState.value = PortfolioUiState.Loading

            portfolioRepository.uploadPortfolio(fileBytes, fileName)
                .onSuccess {
                    _uiState.value = PortfolioUiState.Success("Портфолио успешно загружено")
                }
                .onFailure { e ->
                    _uiState.value = PortfolioUiState.Error(
                        e.message ?: "Не удалось загрузить портфолио"
                    )
                }
        }
    }

    /**
     * Скачивание портфолио (для просмотра / выгрузки)
     */
    fun downloadPortfolio(onSuccess: (ByteArray) -> Unit) {
        viewModelScope.launch {
            _uiState.value = PortfolioUiState.Loading

            portfolioRepository.downloadPortfolio()
                .onSuccess { bytes ->
                    onSuccess(bytes)
                    _uiState.value = PortfolioUiState.Success("Портфолио успешно скачано")
                }
                .onFailure { e ->
                    _uiState.value = PortfolioUiState.Error(
                        e.message ?: "Не удалось скачать портфолио"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = PortfolioUiState.Idle
    }
}