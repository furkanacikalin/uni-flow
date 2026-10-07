package com.uniflow.app.presentation.update_password

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.uniflow.app.core.common.toTurkishErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class UpdatePasswordState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class UpdatePasswordViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = mutableStateOf(UpdatePasswordState())
    val state: State<UpdatePasswordState> = _state

    fun onCurrentPasswordChange(value: String) {
        _state.value = _state.value.copy(currentPassword = value, error = "")
    }

    fun onNewPasswordChange(value: String) {
        _state.value = _state.value.copy(newPassword = value, error = "")
    }

    fun onConfirmNewPasswordChange(value: String) {
        _state.value = _state.value.copy(confirmNewPassword = value, error = "")
    }

    fun updatePassword() {
        val currPass = _state.value.currentPassword.trim()
        val nPass = _state.value.newPassword.trim()
        val cPass = _state.value.confirmNewPassword.trim()

        if (currPass.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen mevcut şifrenizi giriniz.")
            return
        }
        if (nPass.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen yeni şifrenizi giriniz.")
            return
        }
        if (nPass.length < 6) {
            _state.value = _state.value.copy(error = "Yeni şifre en az 6 karakter olmalıdır.")
            return
        }
        if (nPass != cPass) {
            _state.value = _state.value.copy(error = "Yeni şifreler uyuşmuyor.")
            return
        }

        val user = auth.currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            _state.value = _state.value.copy(error = "Kullanıcı oturumu bulunamadı.")
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = "")

        val credential = EmailAuthProvider.getCredential(email, currPass)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(nPass)
                    .addOnSuccessListener {
                        _state.value = _state.value.copy(isLoading = false, isSuccess = true)
                    }
                    .addOnFailureListener { exception ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = exception.toTurkishErrorMessage("Şifre güncellenemedi.")
                        )
                    }
            }
            .addOnFailureListener { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = exception.toTurkishErrorMessage("Mevcut şifreniz hatalı.")
                )
            }
    }
}
