package com.uniflow.app.presentation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.data.repository.AuthRepositoryImpl
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

sealed class MainDestination {
    object Loading : MainDestination()
    object Login : MainDestination()
    object FirstUniversitySelect : MainDestination()
    object CompleteProfile : MainDestination()
    object AdminContainer : MainDestination()
    data class ClubList(val universityId: String, val universityName: String) : MainDestination()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = mutableStateOf<MainDestination>(MainDestination.Loading)
    val destination: State<MainDestination> = _destination

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        if (!authRepository.isUserLoggedIn()) {
            _destination.value = MainDestination.Login
            return
        }

        authRepository.getUserProfile().onEach { profileResult ->
            when (profileResult) {
                is Resource.Loading -> {
                    _destination.value = MainDestination.Loading
                }
                is Resource.Success -> {
                    val profile = profileResult.data
                    if (profile != null && profile.isAdmin) {

                        _destination.value = MainDestination.AdminContainer
                    } else {

                        checkStudentUniversity(profile)
                    }
                }
                is Resource.Error -> {
                    _destination.value = MainDestination.Login
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun checkStudentUniversity(profile: com.uniflow.app.domain.model.UserProfile?) {
        authRepository.getSelectedUniversity().onEach { uniResult ->
            when (uniResult) {
                is Resource.Loading -> {
                    _destination.value = MainDestination.Loading
                }
                is Resource.Success -> {
                    val uni = uniResult.data
                    if (uni == null || uni.id.isBlank() || uni.name.isBlank()) {
                        _destination.value = MainDestination.FirstUniversitySelect
                    } else if (profile == null || !profile.isProfileComplete) {
                        _destination.value = MainDestination.CompleteProfile
                    } else {
                        _destination.value = MainDestination.ClubList(
                            universityId = uni.id,
                            universityName = uni.name
                        )
                    }
                }
                is Resource.Error -> {
                    if (uniResult.message == AuthRepositoryImpl.ERROR_USER_NOT_FOUND) {
                        _destination.value = MainDestination.Login
                    } else {
                        _destination.value = MainDestination.Login
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun logout() {
        authRepository.logout()
        _destination.value = MainDestination.Login
    }
}
