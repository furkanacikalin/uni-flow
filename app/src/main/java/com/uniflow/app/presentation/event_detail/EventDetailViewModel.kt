package com.uniflow.app.presentation.event_detail

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.model.UserProfile
import com.uniflow.app.domain.repository.AuthRepository
import com.uniflow.app.domain.repository.UniversityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class EventDetailState(
    val eventId: String = "",
    val event: Event? = null,
    val isJoined: Boolean = false,
    val isManager: Boolean = false,
    val isManagerChecked: Boolean = false,
    val isLoading: Boolean = false,
    val isJoining: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false,
    val showTicketDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,

    // Attendees dialog
    val showAttendeesDialog: Boolean = false,
    val attendees: List<UserProfile> = emptyList(),
    val isLoadingAttendees: Boolean = false,

    // Form inputs for edit
    val editTitle: String = "",
    val editDescription: String = "",
    val editCategory: String = "TEKNOLOJİ & YAZILIM",
    val editLocation: String = "",
    val editDateText: String = "",
    val editTimeText: String = "",

    val message: String = "",
    val error: String = ""
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: UniversityRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(EventDetailState())
    val state: State<EventDetailState> = _state

    private var managedClubIds = emptySet<String>()
    private var managedClubNames = emptySet<String>()
    private var isAdminUser = false
    private var isClubsChecked = false
    private var isProfileChecked = false

    fun init(eventId: String) {
        if (_state.value.eventId == eventId && _state.value.event != null) return
        isClubsChecked = false
        isProfileChecked = false
        managedClubIds = emptySet()
        managedClubNames = emptySet()
        isAdminUser = false

        _state.value = _state.value.copy(
            eventId = eventId,
            isLoading = true,
            isManager = false,
            isManagerChecked = false
        )
        loadAuthInfo()
        loadEvent(eventId)
        checkJoinedStatus(eventId)
    }

    private fun loadAuthInfo() {
        authRepository.getUserManagedClubs().onEach { res ->
            if (res is Resource.Success) {
                val clubs = res.data ?: emptyList()
                managedClubIds = clubs.map { it.id }.filter { it.isNotBlank() }.toSet()
                managedClubNames = clubs.map { it.name.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
                isClubsChecked = true
                evaluateState()
            } else if (res is Resource.Error) {
                isClubsChecked = true
                evaluateState()
            }
        }.launchIn(viewModelScope)

        authRepository.getUserProfile().onEach { res ->
            if (res is Resource.Success) {
                val profile = res.data
                isAdminUser = profile?.isAdmin == true
                isProfileChecked = true
                evaluateState()
            } else if (res is Resource.Error) {
                isProfileChecked = true
                evaluateState()
            }
        }.launchIn(viewModelScope)
    }

    private fun loadEvent(eventId: String) {
        repository.getEventById(eventId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    val isJoined = _state.value.isJoined
                    val event = result.data
                    val adjustedCount = if (isJoined && (event?.attendeeCount ?: 0) == 0) maxOf(event?.attendeeCount ?: 0, 1) else (event?.attendeeCount ?: 0)
                    _state.value = _state.value.copy(
                        event = event?.copy(attendeeCount = adjustedCount)
                    )
                    evaluateState()
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Etkinlik yüklenemedi.")
                    )
                }
                is Resource.Loading -> {}
            }
        }.launchIn(viewModelScope)
    }

    private fun checkJoinedStatus(eventId: String) {
        repository.isUserJoinedEvent(eventId).onEach { joined ->
            val event = _state.value.event
            val adjustedCount = if (joined && (event?.attendeeCount ?: 0) == 0) maxOf(event?.attendeeCount ?: 0, 1) else (event?.attendeeCount ?: 0)
            _state.value = _state.value.copy(
                isJoined = joined,
                event = event?.copy(attendeeCount = adjustedCount)
            )
        }.launchIn(viewModelScope)
    }

    private fun evaluateState() {
        val event = _state.value.event
        if (event == null) return

        val isAuthChecked = isClubsChecked && isProfileChecked
        val isManagerOfThisClub = isAdminUser ||
            (event.clubId.isNotBlank() && managedClubIds.contains(event.clubId)) ||
            (event.clubName.isNotBlank() && managedClubNames.contains(event.clubName.trim().lowercase()))

        val isManagerChecked = isManagerOfThisClub || isAuthChecked
        val isLoading = !isManagerChecked

        _state.value = _state.value.copy(
            isManager = isManagerOfThisClub,
            isManagerChecked = isManagerChecked,
            isLoading = isLoading
        )
    }

    fun joinEvent() {
        val event = _state.value.event ?: return
        if (event.isPast()) {
            _state.value = _state.value.copy(error = "Tarihi geçmiş etkinliklere katılım sağlanamaz.")
            return
        }
        repository.joinEvent(event).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isJoining = true, error = "", message = "")
                }
                is Resource.Success -> {
                    val currentEvent = _state.value.event
                    val updatedCount = maxOf((currentEvent?.attendeeCount ?: 0) + 1, 1)
                    _state.value = _state.value.copy(
                        isJoining = false,
                        isJoined = true,
                        event = currentEvent?.copy(attendeeCount = updatedCount),
                        showTicketDialog = true,
                        message = "Etkinlik kaydınız ve biletiniz oluşturuldu!"
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isJoining = false,
                        error = result.message.toTurkishErrorMessage("Bilet alınamadı.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun openTicketDialog() {
        _state.value = _state.value.copy(showTicketDialog = true)
    }

    fun closeTicketDialog() {
        _state.value = _state.value.copy(showTicketDialog = false)
    }

    fun openAttendeesDialog() {
        _state.value = _state.value.copy(showAttendeesDialog = true, isLoadingAttendees = true)
        val eventId = _state.value.eventId
        if (eventId.isNotBlank()) {
            repository.getEventAttendees(eventId).onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(
                            attendees = result.data ?: emptyList(),
                            isLoadingAttendees = false
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoadingAttendees = false,
                            error = result.message.toTurkishErrorMessage("Katılımcılar alınamadı.")
                        )
                    }
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(isLoadingAttendees = true)
                    }
                }
            }.launchIn(viewModelScope)
        }
    }

    fun closeAttendeesDialog() {
        _state.value = _state.value.copy(showAttendeesDialog = false)
    }


    fun openEditDialog() {
        val event = _state.value.event ?: return
        _state.value = _state.value.copy(
            showEditDialog = true,
            editTitle = event.title,
            editDescription = event.description,
            editCategory = event.category,
            editLocation = event.location,
            editDateText = event.dateText,
            editTimeText = event.timeText,
            error = "",
            message = ""
        )
    }

    fun closeEditDialog() {
        _state.value = _state.value.copy(showEditDialog = false)
    }

    fun onEditTitleChange(v: String) { _state.value = _state.value.copy(editTitle = v) }
    fun onEditDescriptionChange(v: String) { _state.value = _state.value.copy(editDescription = v) }
    fun onEditCategoryChange(v: String) { _state.value = _state.value.copy(editCategory = v) }
    fun onEditLocationChange(v: String) { _state.value = _state.value.copy(editLocation = v) }
    fun onEditDateTextChange(v: String) { _state.value = _state.value.copy(editDateText = v) }
    fun onEditTimeTextChange(v: String) { _state.value = _state.value.copy(editTimeText = v) }

    fun saveEditedEvent() {
        val currentEvent = _state.value.event ?: return
        val title = _state.value.editTitle.trim()
        val desc = _state.value.editDescription.trim()
        val loc = _state.value.editLocation.trim()
        val date = _state.value.editDateText.trim()
        val time = _state.value.editTimeText.trim()

        if (title.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen etkinlik başlığını giriniz.")
            return
        }
        if (date.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen tarih bilgisini giriniz.")
            return
        }

        val updatedEvent = currentEvent.copy(
            title = title,
            description = desc,
            category = _state.value.editCategory,
            location = loc.ifBlank { "Kampüs Yerleşkesi" },
            dateText = date,
            dateMonth = extractMonth(date),
            dateDay = extractDay(date),
            timeText = time.ifBlank { "14:00" }
        )

        repository.updateEvent(updatedEvent).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isSaving = true, error = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        showEditDialog = false,
                        event = updatedEvent,
                        message = "Etkinlik başarıyla güncellendi!"
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = result.message.toTurkishErrorMessage("Etkinlik güncellenemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }


    fun openDeleteConfirmDialog() {
        _state.value = _state.value.copy(showDeleteConfirmDialog = true)
    }

    fun closeDeleteConfirmDialog() {
        _state.value = _state.value.copy(showDeleteConfirmDialog = false)
    }

    fun deleteEvent() {
        val eventId = _state.value.eventId
        if (eventId.isBlank()) return
        repository.deleteEvent(eventId).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isSaving = true, error = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        showDeleteConfirmDialog = false,
                        isDeleted = true,
                        message = "Etkinlik silindi."
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        showDeleteConfirmDialog = false,
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
