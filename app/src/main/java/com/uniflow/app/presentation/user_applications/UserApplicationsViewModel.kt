package com.uniflow.app.presentation.user_applications

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.ClubApplication
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class UserApplicationsState(
    val isLoading: Boolean = false,
    val applications: List<ClubApplication> = emptyList(),
    val error: String = "",
    val selectedFilter: String = "ALL"
)

@HiltViewModel
class UserApplicationsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(UserApplicationsState())
    val state: State<UserApplicationsState> = _state

    init {
        loadUserApplications()
    }

    fun loadUserApplications() {
        authRepository.getUserClubApplications().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        applications = result.data ?: emptyList(),
                        error = ""
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message ?: "Başvurularınız yüklenirken bir hata oluştu."
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onFilterSelected(filter: String) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }
}
