package com.uniflow.app.presentation.club_detail

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.model.UserProfile
import com.uniflow.app.domain.repository.AuthRepository
import com.uniflow.app.domain.repository.UniversityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class ClubDetailState(
    val club: Club? = null,
    val isJoined: Boolean = false,
    val isLeader: Boolean = false,
    val currentUserId: String = "",
    val members: List<UserProfile> = emptyList(),
    val events: List<Event> = emptyList(),
    val showMembersDialog: Boolean = false,
    val isLoading: Boolean = false,
    val isJoining: Boolean = false,
    val message: String = "",
    val error: String = ""
)

@HiltViewModel
class ClubDetailViewModel @Inject constructor(
    private val repository: UniversityRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(ClubDetailState())
    val state: State<ClubDetailState> = _state

    fun loadClubDetail(clubId: String) {
        _state.value = _state.value.copy(isLoading = true)

        authRepository.getUserProfile().onEach { result ->
            if (result is Resource.Success && result.data != null) {
                val uid = result.data.uid
                val currentClub = _state.value.club
                val isLeader = uid.isNotBlank() && currentClub?.leaderUid?.isNotBlank() == true && currentClub.leaderUid == uid
                _state.value = _state.value.copy(currentUserId = uid, isLeader = isLeader)
            }
        }.launchIn(viewModelScope)

        repository.getClubDetails(clubId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    val c = result.data
                    if (c != null) {
                        val uid = _state.value.currentUserId
                        val isLeader = uid.isNotBlank() && c.leaderUid.isNotBlank() && c.leaderUid == uid
                        _state.value = _state.value.copy(
                            club = c,
                            isLeader = isLeader,
                            isLoading = false
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false, error = "Kulüp bulunamadı.")
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

        repository.isUserJoinedClub(clubId).onEach { joined ->
            _state.value = _state.value.copy(isJoined = joined)
        }.launchIn(viewModelScope)

        repository.getClubMembers(clubId).onEach { result ->
            if (result is Resource.Success && result.data != null) {
                _state.value = _state.value.copy(members = result.data)
            }
        }.launchIn(viewModelScope)

        repository.getEventsByClub(clubId).onEach { result ->
            if (result is Resource.Success && result.data != null) {
                val rawEvents = result.data
                authRepository.getUserBookmarkedEvents().onEach { bookmarkRes ->
                    val bookmarkedIds = if (bookmarkRes is Resource.Success) {
                        (bookmarkRes.data ?: emptyList()).map { it.id }.filter { it.isNotBlank() }.toSet()
                    } else emptySet()
                    val events = rawEvents.map { ev ->
                        ev.copy(isBookmarked = bookmarkedIds.contains(ev.id))
                    }
                    _state.value = _state.value.copy(events = events)
                }.launchIn(viewModelScope)
            }
        }.launchIn(viewModelScope)
    }

    fun toggleBookmark(event: Event) {
        val newBookmarkedState = !event.isBookmarked
        val updatedEvents = _state.value.events.map {
            if (it.id == event.id) it.copy(isBookmarked = newBookmarkedState) else it
        }
        _state.value = _state.value.copy(events = updatedEvents)
        authRepository.toggleBookmarkEvent(event).launchIn(viewModelScope)
    }

    fun joinClub() {
        val club = _state.value.club ?: return
        if (_state.value.isJoined || _state.value.isJoining) return

        _state.value = _state.value.copy(isJoining = true, error = "")
        repository.joinClub(club).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isJoining = false,
                        isJoined = true,
                        message = "Tebrikler! ${club.name} kulübüne katıldınız."
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isJoining = false,
                        error = result.message.toTurkishErrorMessage("Kulübe katılım sağlanamadı.")
                    )
                }
                is Resource.Loading -> {}
            }
        }.launchIn(viewModelScope)
    }

    fun openMembersDialog() { _state.value = _state.value.copy(showMembersDialog = true) }
    fun closeMembersDialog() { _state.value = _state.value.copy(showMembersDialog = false) }
}
