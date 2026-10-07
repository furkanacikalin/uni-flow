package com.uniflow.app.presentation.club_application

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.UserProfile
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class ClubApplicationState(
    val userProfile: UserProfile = UserProfile(),
    val clubName: String = "",
    val clubCategory: String = "AKADEMİK",
    val clubDescription: String = "",
    val documentFileName: String = "",
    val documentUri: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class ClubApplicationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(ClubApplicationState())
    val state: State<ClubApplicationState> = _state

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

    fun onClubNameChange(value: String) {
        _state.value = _state.value.copy(clubName = value, error = "")
    }

    fun onClubCategoryChange(value: String) {
        _state.value = _state.value.copy(clubCategory = value, error = "")
    }

    fun onClubDescriptionChange(value: String) {
        _state.value = _state.value.copy(clubDescription = value, error = "")
    }

    fun onDocumentSelected(fileName: String, uriString: String) {
        _state.value = _state.value.copy(
            documentFileName = fileName,
            documentUri = uriString,
            error = ""
        )
    }

    fun submitApplication() {
        val cName = _state.value.clubName.trim()
        val cDesc = _state.value.clubDescription.trim()
        val docName = _state.value.documentFileName.trim()

        if (cName.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen kurulacak kulübün adını giriniz.")
            return
        }
        if (cDesc.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen kulüp amacını ve açıklamasını giriniz.")
            return
        }
        if (docName.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen üniversiteden aldığınız resmi kulüp kurulma onay belgesini yükleyiniz.")
            return
        }

        authRepository.submitClubApplication(
            clubName = cName,
            clubCategory = _state.value.clubCategory,
            clubDescription = cDesc,
            documentFileName = docName,
            documentUri = _state.value.documentUri
        ).onEach { result ->
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
                        error = result.message.toTurkishErrorMessage("Başvuru gönderilemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
