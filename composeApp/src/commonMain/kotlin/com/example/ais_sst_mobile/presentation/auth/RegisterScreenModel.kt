package com.example.ais_sst_mobile.presentation.auth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.example.ais_sst_mobile.domain.repository.DictionaryRepository
import com.example.ais_sst_mobile.domain.model.Speciality
import com.example.ais_sst_mobile.domain.model.SocialStatus
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Состояние нашего экрана
data class RegisterState(
    val specialities: List<Speciality> = emptyList(),
    val socialStatuses: List<SocialStatus> = emptyList(),
    val isLoading: Boolean = false
)

class RegisterScreenModel(
    private val dictionaryRepository: DictionaryRepository
) : StateScreenModel<RegisterState>(RegisterState()) {

    init {
        // Загружаем данные сразу при создании экрана
        loadDictionaries()
    }

    private fun loadDictionaries() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }

            // Делаем запросы параллельно или последовательно
            val specResult = dictionaryRepository.getSpecialities()
            val statusResult = dictionaryRepository.getSocialStatuses()

            // Обновляем State успешными результатами (или оставляем пустыми при ошибке)
            mutableState.update { state ->
                state.copy(
                    specialities = specResult.getOrNull() ?: emptyList(),
                    socialStatuses = statusResult.getOrNull() ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }
}