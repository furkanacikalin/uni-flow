package com.uniflow.app.presentation.admin_profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.UserProfile
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class AdminProfileState(
    val userProfile: UserProfile = UserProfile(),
    val totalApplicationsCount: Int = 0,
    val totalUniversitiesCount: Int = 208,
    val isLoggedOut: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class AdminProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(AdminProfileState())
    val state: State<AdminProfileState> = _state

    init {
        loadAdminProfile()
    }

    private fun loadAdminProfile() {
        authRepository.getUserProfile().onEach { result ->
            if (result is Resource.Success && result.data != null) {
                _state.value = _state.value.copy(userProfile = result.data)
            }
        }.launchIn(viewModelScope)

        authRepository.getClubApplications().onEach { result ->
            if (result is Resource.Success && result.data != null) {
                _state.value = _state.value.copy(totalApplicationsCount = result.data.size)
            }
        }.launchIn(viewModelScope)
    }

    fun logout() {
        authRepository.logout()
        _state.value = _state.value.copy(isLoggedOut = true)
    }
}
