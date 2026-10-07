package com.uniflow.app.presentation.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.uniflow.app.core.common.toTurkishErrorMessage
import javax.inject.Inject

data class LoginState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isAdmin: Boolean = false,
    val needsUniversitySelection: Boolean = false,
    val needsProfileCompletion: Boolean = false,
    val selectedUniversityId: String? = null,
    val selectedUniversityName: String? = null,
    val error: String = ""
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(LoginState())
    val state: State<LoginState> = _state

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = "")
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = "")
    }

    fun onRememberMeChange(rememberMe: Boolean) {
        _state.value = _state.value.copy(rememberMe = rememberMe)
    }

    fun login() {
        val email = _state.value.email.trim()
        val password = _state.value.password.trim()

        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen e-posta adresinizi girin.")
            return
        }

        if (password.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen şifrenizi girin.")
            return
        }

        authRepository.login(email, password, _state.value.rememberMe).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "")
                }
                is Resource.Success -> {
                    checkUserUniversity()
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Hesap bulunamadı veya şifre hatalı.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun checkUserUniversity() {
        authRepository.getUserProfile().onEach { profileResult ->
            if (profileResult is Resource.Success && profileResult.data?.isAdmin == true) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    isAdmin = true
                )
            } else {
                authRepository.getSelectedUniversity().onEach { result ->
                    when (result) {
                        is Resource.Loading -> {
                            _state.value = _state.value.copy(isLoading = true)
                        }
                        is Resource.Success -> {
                            val uni = result.data
                            if (uni != null && uni.id.isNotBlank()) {
                                checkUserProfile(uni.id, uni.name)
                            } else {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    isSuccess = true,
                                    needsUniversitySelection = true
                                )
                            }
                        }
                        is Resource.Error -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                isSuccess = true,
                                needsUniversitySelection = true
                            )
                        }
                    }
                }.launchIn(viewModelScope)
            }
        }.launchIn(viewModelScope)
    }

    private fun checkUserProfile(uniId: String, uniName: String) {
        authRepository.getUserProfile().onEach { result ->
            when (result) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    val profile = result.data
                    val needsProfile = profile == null || !profile.isProfileComplete
                    val isAdmin = profile?.isAdmin ?: false
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        isAdmin = isAdmin,
                        needsUniversitySelection = false,
                        needsProfileCompletion = needsProfile,
                        selectedUniversityId = uniId,
                        selectedUniversityName = uniName
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        needsUniversitySelection = false,
                        needsProfileCompletion = true,
                        selectedUniversityId = uniId,
                        selectedUniversityName = uniName
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
