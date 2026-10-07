package com.uniflow.app.presentation.club_management

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.model.UserProfile
import com.uniflow.app.domain.repository.UniversityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class ClubManagementState(
    val clubId: String = "",
    val club: Club? = null,
    val events: List<Event> = emptyList(),
    val members: List<UserProfile> = emptyList(),
    val isMembersLoading: Boolean = false,
    val selectedTab: Int = 0,


    val clubName: String = "",
    val clubCategory: String = "",
    val clubDescription: String = "",
    val socialLinks: Map<String, String> = emptyMap(),


    val showSettingsDialog: Boolean = false,
    val showAddLinkDialog: Boolean = false,
    val selectedPlatform: String = "Instagram",
    val linkUrlInput: String = "",


    val showEventDialog: Boolean = false,
    val editingEvent: Event? = null,
    val eventTitle: String = "",
    val eventDescription: String = "",
    val eventCategory: String = "TEKNOLOJİ & YAZILIM",
    val eventLocation: String = "",
    val eventDateText: String = "",
    val eventTimeText: String = "",


    val deletingEventId: String? = null,

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String = "",
    val error: String = ""
)

@HiltViewModel
class ClubManagementViewModel @Inject constructor(
    private val repository: UniversityRepository
) : ViewModel() {

    private val _state = mutableStateOf(ClubManagementState())
    val state: State<ClubManagementState> = _state

    fun init(clubId: String) {
        if (_state.value.clubId == clubId && _state.value.club != null) return
        _state.value = _state.value.copy(clubId = clubId, isLoading = true)
        loadClubDetails(clubId)
        loadEvents(clubId)
        loadMembers(clubId)
    }

    private fun loadMembers(clubId: String) {
        repository.getClubMembers(clubId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        members = result.data ?: emptyList(),
                        isMembersLoading = false
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(isMembersLoading = false)
                }
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isMembersLoading = true)
                }
            }
        }.launchIn(viewModelScope)
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
                        error = result.message.toTurkishErrorMessage("Kulüp detayı yüklenemedi.")
                    )
                }
                is Resource.Loading -> {}
            }
        }.launchIn(viewModelScope)
    }

    private fun loadEvents(clubId: String) {
        repository.getEventsByClub(clubId).onEach { result ->
            if (result is Resource.Success) {
                _state.value = _state.value.copy(events = result.data ?: emptyList())
            }
        }.launchIn(viewModelScope)
    }

    fun onTabSelected(index: Int) {
        _state.value = _state.value.copy(selectedTab = index)
    }

    fun openSettingsDialog() {
        _state.value = _state.value.copy(showSettingsDialog = true)
    }

    fun closeSettingsDialog() {
        _state.value = _state.value.copy(showSettingsDialog = false)
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

    fun onClubNameChange(value: String) {
        _state.value = _state.value.copy(clubName = value, error = "", message = "")
    }

    fun onClubCategoryChange(value: String) {
        _state.value = _state.value.copy(clubCategory = value, error = "", message = "")
    }

    fun onClubDescriptionChange(value: String) {
        _state.value = _state.value.copy(clubDescription = value, error = "", message = "")
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
                    val updatedClub = _state.value.club?.copy(
                        name = name,
                        category = category,
                        description = desc,
                        socialLinks = links
                    )
                    _state.value = _state.value.copy(
                        club = updatedClub,
                        isSaving = false,
                        showSettingsDialog = false,
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


    fun openCreateEventDialog() {
        _state.value = _state.value.copy(
            showEventDialog = true,
            editingEvent = null,
            eventTitle = "",
            eventDescription = "",
            eventCategory = "TEKNOLOJİ & YAZILIM",
            eventLocation = "",
            eventDateText = "",
            eventTimeText = "",
            error = "",
            message = ""
        )
    }

    fun openEditEventDialog(event: Event) {
        _state.value = _state.value.copy(
            showEventDialog = true,
            editingEvent = event,
            eventTitle = event.title,
            eventDescription = event.description,
            eventCategory = event.category,
            eventLocation = event.location,
            eventDateText = event.dateText,
            eventTimeText = event.timeText,
            error = "",
            message = ""
        )
    }

    fun closeEventDialog() {
        _state.value = _state.value.copy(showEventDialog = false)
    }

    fun onEventTitleChange(v: String) { _state.value = _state.value.copy(eventTitle = v) }
    fun onEventDescriptionChange(v: String) { _state.value = _state.value.copy(eventDescription = v) }
    fun onEventCategoryChange(v: String) { _state.value = _state.value.copy(eventCategory = v) }
    fun onEventLocationChange(v: String) { _state.value = _state.value.copy(eventLocation = v) }
    fun onEventDateTextChange(v: String) { _state.value = _state.value.copy(eventDateText = v) }
    fun onEventTimeTextChange(v: String) { _state.value = _state.value.copy(eventTimeText = v) }

    fun saveEvent() {
        val title = _state.value.eventTitle.trim()
        val desc = _state.value.eventDescription.trim()
        val loc = _state.value.eventLocation.trim()
        val date = _state.value.eventDateText.trim()
        val time = _state.value.eventTimeText.trim()

        if (title.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen etkinlik başlığını giriniz.")
            return
        }
        if (date.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen tarih bilgisini giriniz.")
            return
        }

        val existing = _state.value.editingEvent
        val club = _state.value.club

        val targetEvent = Event(
            id = existing?.id ?: "",
            universityId = club?.universityId ?: existing?.universityId ?: "",
            clubId = _state.value.clubId,
            clubName = club?.name ?: existing?.clubName ?: "Kulüp Etkinliği",
            title = title,
            description = desc,
            category = _state.value.eventCategory,
            location = loc.ifBlank { "Kampüs Yerleşkesi" },
            dateText = date,
            dateMonth = extractMonth(date),
            dateDay = extractDay(date),
            timeText = time.ifBlank { "14:00" },
            attendeeCount = existing?.attendeeCount ?: 0,
            isFeatured = existing?.isFeatured ?: false,
            isFree = true
        )

        val flow = if (existing != null) repository.updateEvent(targetEvent) else repository.createEvent(targetEvent)
        flow.onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isSaving = true, error = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        showEventDialog = false,
                        message = if (existing != null) "Etkinlik başarıyla güncellendi!" else "Yeni etkinlik başarıyla oluşturuldu!"
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = result.message.toTurkishErrorMessage("Etkinlik kaydedilemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun setDeletingEventId(id: String?) {
        _state.value = _state.value.copy(deletingEventId = id)
    }

    fun deleteEvent() {
        val id = _state.value.deletingEventId ?: return
        repository.deleteEvent(id).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isSaving = true)
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        deletingEventId = null,
                        message = "Etkinlik silindi."
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        deletingEventId = null,
                        error = result.message.toTurkishErrorMessage("Etkinlik silinemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun extractDay(dateStr: String): String {
        val numbers = Regex("\\d+").find(dateStr)?.value
        return numbers ?: "15"
    }

    private fun extractMonth(dateStr: String): String {
        val lower = dateStr.lowercase()
        return when {
            lower.contains("ocak") -> "OCA"
            lower.contains("şubat") || lower.contains("subat") -> "SUB"
            lower.contains("mart") -> "MAR"
            lower.contains("nisan") -> "NIS"
            lower.contains("mayıs") || lower.contains("mayis") -> "MAY"
            lower.contains("haziran") -> "HAZ"
            lower.contains("temmuz") -> "TEM"
            lower.contains("ağustos") || lower.contains("agustos") -> "AGU"
            lower.contains("eylül") || lower.contains("eylul") -> "EYL"
            lower.contains("ekim") -> "EKI"
            lower.contains("kasım") || lower.contains("kasim") -> "KAS"
            lower.contains("aralık") || lower.contains("aralik") -> "ARA"
            else -> "MAY"
        }
    }
}
