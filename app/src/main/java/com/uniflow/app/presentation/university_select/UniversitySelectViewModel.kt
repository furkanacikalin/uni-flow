package com.uniflow.app.presentation.university_select

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.University
import com.uniflow.app.domain.usecase.GetUniversitiesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class UniversitySelectState(
    val isLoading: Boolean = false,
    val universities: List<University> = emptyList(),
    val error: String = ""
)

@HiltViewModel
class UniversitySelectViewModel @Inject constructor(
    private val getUniversitiesUseCase: GetUniversitiesUseCase
) : ViewModel() {

    private val _state = mutableStateOf(UniversitySelectState())
    val state: State<UniversitySelectState> = _state

    init {
        loadUniversities()
    }

    private fun loadUniversities() {
        getUniversitiesUseCase().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = UniversitySelectState(isLoading = true)
                }
                is Resource.Success -> {
                    _state.value = UniversitySelectState(universities = result.data ?: emptyList())
                }
                is Resource.Error -> {
                    _state.value = UniversitySelectState(error = result.message ?: "Hata oluştu.")
                }
            }
        }.launchIn(viewModelScope)
    }
}