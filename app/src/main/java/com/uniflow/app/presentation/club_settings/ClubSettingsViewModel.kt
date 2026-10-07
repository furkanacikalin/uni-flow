package com.uniflow.app.presentation.club_settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.repository.UniversityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class ClubSettingsState(
    val clubId: String = "",
    val club: Club? = null,
    val clubName: String = "",
    val clubCategory: String = "",
    val clubDescription: String = "",
    val socialLinks: Map<String, String> = emptyMap(),

    // Add Link Dialog State
    val showAddLinkDialog: Boolean = false,
    val selectedPlatform: String = "Instagram",
    val linkUrlInput: String = "",

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val message: String = "",
    val error: String = ""
)

@HiltViewModel
class ClubSettingsViewModel @Inject constructor(
    private val repository: UniversityRepository
) : ViewModel() {

    private val _state = mutableStateOf(ClubSettingsState())
    val state: State<ClubSettingsState> = _state

    fun init(clubId: String) {
        if (_state.value.clubId == clubId && _state.value.club != null) return
        _state.value = _state.value.copy(clubId = clubId, isLoading = true)
        loadClubDetails(clubId)
    }

    private fun loadClubDetails(clubId: String) {
        repository.getClubDetails(clubId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    val c = result.data
                    if (c != null) {
                        val links = if (c.socialLinks.isNotEmpty()) {
                            c.socialLinks
                        } else if (c.instagramUrl.isNotBlank()) {
                            mapOf("Instagram" to c.instagramUrl)
                        } else {
                            emptyMap()
                        }
                        _state.value = _state.value.copy(
                            club = c,
                            clubName = c.name,
                            clubCategory = c.category,
                            clubDescription = c.description,
                            socialLinks = links,
                            isLoading = false
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false)
                    }
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Kulüp detayları alınamadı.")
                    )
                }
                is Resource.Loading -> {}
            }
        }.launchIn(viewModelScope)
    }

    fun onClubNameChange(value: String) {
        _state.value = _state.value.copy(clubName = value, error = "", message = "")
    }

    fun onClubCategoryChange(value: String) {
        _state.value = _state.value.copy(clubCategory = value, error = "", message = "")
    }

    fun onClubDescriptionChange(value: String) {
        _state.value = _state.value.copy(clubDescription = value, error = "", message = "")
    }

    fun openAddLinkDialog() {
        _state.value = _state.value.copy(showAddLinkDialog = true, linkUrlInput = "")
    }

    fun closeAddLinkDialog() {
        _state.value = _state.value.copy(showAddLinkDialog = false, linkUrlInput = "")
    }

    fun onPlatformSelected(platform: String) {
        _state.value = _state.value.copy(selectedPlatform = platform)
    }

    fun onLinkUrlInputChange(url: String) {
        _state.value = _state.value.copy(linkUrlInput = url)
    }

    fun addOrUpdateSocialLink() {
        val platform = _state.value.selectedPlatform
        val url = _state.value.linkUrlInput.trim()
        if (url.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen geçerli bir link girin.")
            return
        }
        val updated = _state.value.socialLinks.toMutableMap()
        updated[platform] = url
        _state.value = _state.value.copy(
            socialLinks = updated,
            showAddLinkDialog = false,
            linkUrlInput = "",
            error = ""
        )
    }

    fun removeSocialLink(platform: String) {
        val updated = _state.value.socialLinks.toMutableMap()
        updated.remove(platform)
        _state.value = _state.value.copy(socialLinks = updated)
    }

    fun saveClubInfo() {
        val cid = _state.value.clubId
        val name = _state.value.clubName.trim()
        val category = _state.value.clubCategory.trim()
        val desc = _state.value.clubDescription.trim()
        val links = _state.value.socialLinks

        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Kulüp adı boş bırakılamaz.")
            return
        }

        repository.updateClubDetails(cid, name, category, desc, links).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isSaving = true, error = "", message = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        isSavedSuccess = true,
                        message = "Kulüp ayarları başarıyla güncellendi!"
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = result.message.toTurkishErrorMessage("Kulüp güncellenemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
