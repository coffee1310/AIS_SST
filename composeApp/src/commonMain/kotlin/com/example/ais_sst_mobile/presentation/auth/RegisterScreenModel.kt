package com.example.ais_sst_mobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.domain.model.SocialStatus
import com.example.ais_sst_mobile.domain.model.Speciality
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterState(
    val specialities: List<Speciality> = emptyList(),
    val socialStatuses: List<SocialStatus> = emptyList(),
    val isLoading: Boolean = false
)

class RegisterScreenModel(
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    init {
        loadDictionaries()
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val specResult = dictionaryRepository.getSpecialities()
            val statusResult = dictionaryRepository.getSocialStatuses()

            _state.update { state ->
                state.copy(
                    specialities = specResult.getOrNull() ?: emptyList(),
                    socialStatuses = statusResult.getOrNull() ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }
}