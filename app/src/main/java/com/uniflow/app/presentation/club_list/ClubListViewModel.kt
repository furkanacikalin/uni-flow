package com.uniflow.app.presentation.club_list

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.usecase.GetClubsByUniversityUseCase
import com.uniflow.app.domain.usecase.GetEventsByUniversityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class ClubListState(
    val isLoading: Boolean = false,
    val clubs: List<Club> = emptyList(),
    val events: List<Event> = emptyList(),
    val currentUserId: String = "",
    val error: String = ""
)

@HiltViewModel
class ClubListViewModel @Inject constructor(
    private val getClubsByUniversityUseCase: GetClubsByUniversityUseCase,
    private val getEventsByUniversityUseCase: GetEventsByUniversityUseCase,
    private val firebaseAuth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(ClubListState(currentUserId = firebaseAuth.currentUser?.uid ?: ""))
    val state: State<ClubListState> = _state

    init {
        savedStateHandle.get<String>("universityId")?.let { universityId ->
            loadData(universityId)
        }
    }

    private fun loadData(universityId: String) {
        val uid = firebaseAuth.currentUser?.uid ?: ""
        _state.value = _state.value.copy(isLoading = true, currentUserId = uid)

        getClubsByUniversityUseCase(universityId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        clubs = result.data ?: emptyList(),
                        isLoading = false
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message ?: "Kulüpler yüklenemedi.",
                        isLoading = false
                    )
                }
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }.launchIn(viewModelScope)

        getEventsByUniversityUseCase(universityId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        events = result.data ?: emptyList()
                    )
                }
                is Resource.Error -> {}
                is Resource.Loading -> {}
            }
        }.launchIn(viewModelScope)
    }
}