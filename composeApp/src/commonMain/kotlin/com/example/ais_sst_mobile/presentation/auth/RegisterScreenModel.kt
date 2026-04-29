package com.example.ais_sst_mobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ais_sst_mobile.domain.model.Group // <-- ТВОЙ НОВЫЙ ИМПОРТ
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
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,

    val loginPart: String = "",
    val selectedDomain: String = "@edu.fa.ru",
    val loginError: String? = null
)

class RegisterScreenModel(
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    init {
        loadDictionaries()
    }

    fun updateLoginData(login: String, domain: String) {
        _state.update { it.copy(loginPart = login, selectedDomain = domain) }
        validateLogin(login, domain)
    }

    private fun validateLogin(login: String, domain: String) {
        if (login.isBlank()) {
            _state.update { it.copy(loginError = null) }
            return
        }

        val hasRussianLetters = login.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }
        if (hasRussianLetters) {
            _state.update { it.copy(loginError = "Логин не должен содержать русские буквы") }
            return
        }

        val error = when (domain) {
            "@edu.fa.ru" -> {
                if (login.length != 6 || !login.all { it.isDigit() }) {
                    "Студбилет должен состоять ровно из 6 цифр"
                } else null
            }
            "@fa.ru" -> {
                null
            }
            else -> null
        }

        _state.update { it.copy(loginError = error) }
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val specResult = dictionaryRepository.getSpecialities()
            val statusResult = dictionaryRepository.getSocialStatuses()
            val groupResult = dictionaryRepository.getGroups()

            _state.update { state ->
                state.copy(
                    specialities = specResult.getOrNull() ?: emptyList(),
                    socialStatuses = statusResult.getOrNull() ?: emptyList(),
                    groups = groupResult.getOrNull() ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }
}