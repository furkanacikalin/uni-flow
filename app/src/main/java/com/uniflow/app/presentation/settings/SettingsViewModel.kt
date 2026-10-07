package com.uniflow.app.presentation.settings

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

data class SettingsState(
    val userProfile: UserProfile = UserProfile(),
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(SettingsState())
    val state: State<SettingsState> = _state

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        authRepository.getUserProfile().onEach { result ->
            if (result is Resource.Success && result.data != null) {
                _state.value = _state.value.copy(userProfile = result.data)
            }
        }.launchIn(viewModelScope)
    }

    fun logout() {
        authRepository.logout()
        _state.value = _state.value.copy(isLoggedOut = true)
    }
}
