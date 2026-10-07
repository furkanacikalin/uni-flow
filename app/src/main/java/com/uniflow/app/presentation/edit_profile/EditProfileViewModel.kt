package com.uniflow.app.presentation.edit_profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class EditProfileState(
    val initialFirstName: String = "",
    val initialLastName: String = "",
    val initialProfileImageUrl: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val selectedImageUri: String? = null,
    val isPhotoRemoved: Boolean = false,
    val isLoading: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val message: String = "",
    val error: String = "",
    val showPhotoOptionsBottomSheet: Boolean = false,
    val showEmailDialog: Boolean = false,
    val showPasswordDialog: Boolean = false
) {
    val isChanged: Boolean
        get() {
            val nameChanged = firstName.trim() != initialFirstName.trim() || lastName.trim() != initialLastName.trim()
            val photoChanged = selectedImageUri != null || (isPhotoRemoved && initialProfileImageUrl.isNotEmpty())
            return nameChanged || photoChanged
        }
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(EditProfileState())
    val state: State<EditProfileState> = _state

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        authRepository.getUserProfile().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "")
                }
                is Resource.Success -> {
                    val profile = result.data
                    if (profile != null) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            initialFirstName = profile.firstName,
                            initialLastName = profile.lastName,
                            initialProfileImageUrl = profile.profileImageUrl,
                            firstName = profile.firstName,
                            lastName = profile.lastName,
                            email = profile.email,
                            profileImageUrl = profile.profileImageUrl,
                            selectedImageUri = null,
                            isPhotoRemoved = false
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false)
                    }
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Profil yüklenemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onFirstNameChange(value: String) {
        _state.value = _state.value.copy(firstName = value, error = "", message = "")
    }

    fun onLastNameChange(value: String) {
        _state.value = _state.value.copy(lastName = value, error = "", message = "")
    }

    fun onPhotoSelected(uriString: String) {
        _state.value = _state.value.copy(
            selectedImageUri = uriString,
            isPhotoRemoved = false,
            showPhotoOptionsBottomSheet = false,
            error = "",
            message = ""
        )
    }

    fun onRemovePhoto() {
        _state.value = _state.value.copy(
            profileImageUrl = "",
            selectedImageUri = null,
            isPhotoRemoved = true,
            showPhotoOptionsBottomSheet = false,
            error = "",
            message = ""
        )
    }

    fun setShowPhotoOptions(show: Boolean) {
        _state.value = _state.value.copy(showPhotoOptionsBottomSheet = show)
    }

    fun setShowEmailDialog(show: Boolean) {
        _state.value = _state.value.copy(showEmailDialog = show, error = "", message = "")
    }

    fun setShowPasswordDialog(show: Boolean) {
        _state.value = _state.value.copy(showPasswordDialog = show, error = "", message = "")
    }

    fun saveChanges() {
        val fName = _state.value.firstName.trim()
        val lName = _state.value.lastName.trim()

        if (fName.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen adınızı giriniz.")
            return
        }
        if (lName.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen soyadınızı giriniz.")
            return
        }

        val targetPhotoUrl = when {
            _state.value.selectedImageUri != null -> _state.value.selectedImageUri
            _state.value.isPhotoRemoved -> ""
            else -> null
        }

        authRepository.updateEditableProfile(
            firstName = fName,
            lastName = lName,
            profileImageUrl = targetPhotoUrl
        ).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "", message = "")
                }
                is Resource.Success -> {
                    val finalPhotoUrl = targetPhotoUrl ?: _state.value.profileImageUrl
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSaveSuccess = true,
                        initialFirstName = fName,
                        initialLastName = lName,
                        initialProfileImageUrl = finalPhotoUrl,
                        profileImageUrl = finalPhotoUrl,
                        selectedImageUri = null,
                        isPhotoRemoved = false,
                        message = "Profil değişiklikleri başarıyla kaydedildi!"
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Profil güncellenemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun updateUserEmail(newEmail: String) {
        if (newEmail.trim().isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen e-posta adresinizi giriniz.")
            return
        }

        authRepository.updateUserEmail(newEmail.trim()).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "", message = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        email = newEmail.trim(),
                        showEmailDialog = false,
                        message = "E-posta adresi başarıyla güncellendi!"
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("E-posta güncellenemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun updateUserPassword(newPassword: String, confirmPassword: String) {
        if (newPassword.trim().isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen yeni şifrenizi giriniz.")
            return
        }
        if (newPassword.trim() != confirmPassword.trim()) {
            _state.value = _state.value.copy(error = "Şifreler uyuşmuyor.")
            return
        }
        if (newPassword.trim().length < 6) {
            _state.value = _state.value.copy(error = "Şifre en az 6 karakter olmalıdır.")
            return
        }

        authRepository.updateUserPassword(newPassword.trim()).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "", message = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        showPasswordDialog = false,
                        message = "Şifreniz başarıyla güncellendi!"
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Şifre güncellenemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
