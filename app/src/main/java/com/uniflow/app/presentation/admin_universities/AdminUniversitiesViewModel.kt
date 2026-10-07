package com.uniflow.app.presentation.admin_universities

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
import java.util.Locale
import javax.inject.Inject

data class AdminUniversitiesState(
    val universities: List<University> = emptyList(),
    val filteredUniversities: List<University> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class AdminUniversitiesViewModel @Inject constructor(
    private val getUniversitiesUseCase: GetUniversitiesUseCase
) : ViewModel() {

    private val _state = mutableStateOf(AdminUniversitiesState())
    val state: State<AdminUniversitiesState> = _state

    init {
        loadUniversities()
    }

    fun loadUniversities() {
        getUniversitiesUseCase().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "")
                }
                is Resource.Success -> {
                    val list = result.data ?: emptyList()
                    _state.value = _state.value.copy(
                        universities = list,
                        filteredUniversities = filterList(list, _state.value.searchQuery),
                        isLoading = false
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Üniversiteler yüklenemedi."
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            filteredUniversities = filterList(_state.value.universities, query)
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
}
