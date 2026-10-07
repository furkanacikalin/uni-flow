package com.uniflow.app.presentation.admin_applications

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.ClubApplication
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class AdminApplicationsState(
    val applications: List<ClubApplication> = emptyList(),
    val selectedFilter: String = "PENDING",
    val isLoading: Boolean = false,
    val updatingId: String? = null,
    val error: String = ""
)

@HiltViewModel
class AdminApplicationsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(AdminApplicationsState())
    val state: State<AdminApplicationsState> = _state

    init {
        loadApplications()
    }

    fun loadApplications() {
        authRepository.getClubApplications().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        applications = result.data ?: emptyList(),
                        isLoading = false
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Başvurular yüklenemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onFilterSelected(filter: String) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }

    fun updateStatus(application: ClubApplication, newStatus: String) {
        authRepository.updateApplicationStatus(application.id, newStatus, application).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(updatingId = application.id)
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(updatingId = null)
                    loadApplications()
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        updatingId = null,
                        error = result.message.toTurkishErrorMessage("İşlem gerçekleştirilemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
