package com.uniflow.app.presentation.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.model.ClubApplication
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.model.EventTicket
import com.uniflow.app.domain.model.UserProfile
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class ProfileState(
    val userProfile: UserProfile = UserProfile(),
    val tickets: List<EventTicket> = emptyList(),
    val joinedClubs: List<Club> = emptyList(),
    val managedClubs: List<Club> = emptyList(),
    val userApplications: List<ClubApplication> = emptyList(),
    val bookmarkedEvents: List<Event> = emptyList(),
    val pastEvents: List<EventTicket> = emptyList(),
    val selectedTab: String = "Kayıtlarım",
    val isLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(ProfileState())
    val state: State<ProfileState> = _state

    init {
        loadData()
    }

    fun loadData() {
        _state.value = _state.value.copy(isLoading = true)


        authRepository.getUserProfile().onEach { result ->
            if (result is Resource.Success && result.data != null) {
                _state.value = _state.value.copy(
                    userProfile = result.data,
                    isLoading = false
                )
            } else if (result is Resource.Error) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }.launchIn(viewModelScope)


        authRepository.getUserJoinedClubs().onEach { result ->
            if (result is Resource.Success) {
                val list = result.data ?: emptyList()
                _state.value = _state.value.copy(joinedClubs = list)
            }
        }.launchIn(viewModelScope)


        authRepository.getUserManagedClubs().onEach { result ->
            if (result is Resource.Success) {
                val list = result.data ?: emptyList()
                _state.value = _state.value.copy(managedClubs = list)
            }
        }.launchIn(viewModelScope)


        authRepository.getUserClubApplications().onEach { result ->
            if (result is Resource.Success) {
                val list = result.data ?: emptyList()
                _state.value = _state.value.copy(userApplications = list)
            }
        }.launchIn(viewModelScope)


        authRepository.getUserTickets().onEach { result ->
            if (result is Resource.Success) {
                val list = result.data ?: emptyList()
                _state.value = _state.value.copy(tickets = list)
            }
        }.launchIn(viewModelScope)


        authRepository.getUserBookmarkedEvents().onEach { result ->
            if (result is Resource.Success) {
                val list = result.data ?: emptyList()
                _state.value = _state.value.copy(bookmarkedEvents = list)
            }
        }.launchIn(viewModelScope)


        authRepository.getUserPastEvents().onEach { result ->
            if (result is Resource.Success) {
                val list = result.data ?: emptyList()
                _state.value = _state.value.copy(pastEvents = list)
            }
        }.launchIn(viewModelScope)
    }

    fun onTabSelected(tab: String) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun leaveClub(clubId: String) {
        authRepository.leaveClub(clubId).launchIn(viewModelScope)
    }

    fun toggleBookmark(event: Event) {
        authRepository.toggleBookmarkEvent(event).launchIn(viewModelScope)
    }

    fun logout() {
        authRepository.logout()
        _state.value = _state.value.copy(isLoggedOut = true)
    }
}
