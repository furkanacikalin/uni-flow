package com.uniflow.app.presentation.register

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

data class RegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(RegisterState())
    val state: State<RegisterState> = _state

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = "")
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = "")
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _state.value = _state.value.copy(confirmPassword = confirmPassword, error = "")
    }

    fun register() {
        val email = _state.value.email.trim()
        val password = _state.value.password.trim()
        val confirmPassword = _state.value.confirmPassword.trim()

        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen e-posta adresinizi girin.")
            return
        }

        if (password.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen şifrenizi girin.")
            return
        }

        if (password.length < 6) {
            _state.value = _state.value.copy(error = "Şifre en az 6 karakter olmalıdır.")
            return
        }

        if (password != confirmPassword) {
            _state.value = _state.value.copy(error = "Şifreler eşleşmiyor.")
            return
        }

        authRepository.register(email, password).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true, error = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Kayıt sırasında bir hata oluştu.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
