package com.uniflow.app.presentation.first_university_select

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.University
import com.uniflow.app.domain.usecase.GetUniversitiesUseCase
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

data class FirstUniversitySelectState(
    val allUniversities: List<University> = emptyList(),
    val filteredUniversities: List<University> = emptyList(),
    val searchQuery: String = "",
    val selectedUniversity: University? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class FirstUniversitySelectViewModel @Inject constructor(
    private val getUniversitiesUseCase: GetUniversitiesUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(FirstUniversitySelectState())
    val state: State<FirstUniversitySelectState> = _state

    init {
        loadUniversities()
    }

    private fun loadUniversities() {
        getUniversitiesUseCase().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "")
                }
                is Resource.Success -> {
                    val list = result.data ?: emptyList()
                    _state.value = _state.value.copy(
                        allUniversities = list,
                        filteredUniversities = filterList(list, _state.value.searchQuery),
                        isLoading = false
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Üniversiteler yüklenemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            filteredUniversities = filterList(_state.value.allUniversities, query)
        )
    }

    private fun filterList(list: List<University>, query: String): List<University> {
        if (query.isBlank()) return list
        val q = query.lowercase(Locale("tr", "TR")).trim()
        return list.filter { uni ->
            uni.name.lowercase(Locale("tr", "TR")).contains(q) ||
            uni.city.lowercase(Locale("tr", "TR")).contains(q)
        }
    }

    fun onUniversitySelected(university: University) {
        _state.value = _state.value.copy(selectedUniversity = university, error = "")
    }

    fun saveAndContinue() {
        val selected = _state.value.selectedUniversity
        if (selected == null) {
            _state.value = _state.value.copy(error = "Lütfen bir üniversite seçiniz.")
            return
        }

        authRepository.saveSelectedUniversity(selected).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isSaving = true, error = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        isSuccess = true
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = result.message.toTurkishErrorMessage("Seçim kaydedilemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
