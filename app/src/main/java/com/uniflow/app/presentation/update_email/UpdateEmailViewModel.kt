package com.uniflow.app.presentation.update_email

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

data class UpdateEmailState(
    val currentEmail: String = "",
    val newEmail: String = "",
    val confirmNewEmail: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class UpdateEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(UpdateEmailState())
    val state: State<UpdateEmailState> = _state

    init {
        loadCurrentEmail()
    }

    private fun loadCurrentEmail() {
        authRepository.getUserProfile().onEach { result ->
            if (result is Resource.Success && result.data != null) {
                _state.value = _state.value.copy(currentEmail = result.data.email)
            }
        }.launchIn(viewModelScope)
    }

    fun onNewEmailChange(value: String) {
        _state.value = _state.value.copy(newEmail = value, error = "")
    }

    fun onConfirmNewEmailChange(value: String) {
        _state.value = _state.value.copy(confirmNewEmail = value, error = "")
    }

    fun updateEmail() {
        val nEmail = _state.value.newEmail.trim()
        val cEmail = _state.value.confirmNewEmail.trim()

        if (nEmail.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen yeni mail adresinizi giriniz.")
            return
        }
        if (nEmail != cEmail) {
            _state.value = _state.value.copy(error = "Girilen yeni mail adresleri uyuşmuyor.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(nEmail).matches()) {
            _state.value = _state.value.copy(error = "Lütfen geçerli bir e-posta adresi giriniz.")
            return
        }

        authRepository.updateUserEmail(nEmail).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        currentEmail = nEmail
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("E-posta adresi güncellenemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
