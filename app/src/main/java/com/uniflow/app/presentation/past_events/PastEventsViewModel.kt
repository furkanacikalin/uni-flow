package com.uniflow.app.presentation.past_events

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.repository.AuthRepository
import com.uniflow.app.domain.usecase.GetEventsByUniversityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class PastEventsState(
    val pastEvents: List<Event> = emptyList(),
    val filteredPastEvents: List<Event> = emptyList(),
    val userJoinedEventIds: Set<String> = emptySet(),
    val userBookmarkedEventIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class PastEventsViewModel @Inject constructor(
    private val getEventsByUniversityUseCase: GetEventsByUniversityUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(PastEventsState())
    val state: State<PastEventsState> = _state

    fun loadEvents(universityId: String) {
        authRepository.getUserTickets().onEach { ticketRes ->
            if (ticketRes is Resource.Success) {
                val ticketIds = (ticketRes.data ?: emptyList()).map { it.id }.filter { it.isNotBlank() }.toSet()
                _state.value = _state.value.copy(userJoinedEventIds = ticketIds)
                updateFilteredState(_state.value.pastEvents, _state.value.searchQuery)
            }
        }.launchIn(viewModelScope)

        authRepository.getUserBookmarkedEvents().onEach { bookmarkRes ->
            if (bookmarkRes is Resource.Success) {
                val bookmarkedIds = (bookmarkRes.data ?: emptyList()).map { it.id }.filter { it.isNotBlank() }.toSet()
                val updatedEvents = _state.value.pastEvents.map { ev ->
                    ev.copy(isBookmarked = bookmarkedIds.contains(ev.id))
                }
                _state.value = _state.value.copy(userBookmarkedEventIds = bookmarkedIds)
                updateFilteredState(updatedEvents, _state.value.searchQuery)
            }
        }.launchIn(viewModelScope)

        getEventsByUniversityUseCase(universityId).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
                is Resource.Success -> {
                    val rawEvents = result.data ?: emptyList()
                    val joinedIds = _state.value.userJoinedEventIds
                    val bookmarkedIds = _state.value.userBookmarkedEventIds

                    val pastList = rawEvents
                        .filter { it.isPast() }
                        .map { ev ->
                            var updated = if (ev.id.isNotBlank() && joinedIds.contains(ev.id) && ev.attendeeCount == 0) ev.copy(attendeeCount = 1) else ev
                            if (ev.id.isNotBlank() && bookmarkedIds.contains(ev.id)) {
                                updated = updated.copy(isBookmarked = true)
                            }
                            updated
                        }
                        .sortedByDescending { it.getEndMillis() }

                    _state.value = _state.value.copy(isLoading = false, pastEvents = pastList)
                    updateFilteredState(pastList, _state.value.searchQuery)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        pastEvents = emptyList(),
                        filteredPastEvents = emptyList(),
                        isLoading = false
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        updateFilteredState(_state.value.pastEvents, query)
    }

    fun toggleBookmark(event: Event) {
        val newBookmarkedState = !event.isBookmarked
        val updatedEvents = _state.value.pastEvents.map {
            if (it.id == event.id) it.copy(isBookmarked = newBookmarkedState) else it
        }
        val updatedBookmarkedIds = if (newBookmarkedState) {
            _state.value.userBookmarkedEventIds + event.id
        } else {
            _state.value.userBookmarkedEventIds - event.id
        }

        _state.value = _state.value.copy(userBookmarkedEventIds = updatedBookmarkedIds)
        updateFilteredState(updatedEvents, _state.value.searchQuery)

        authRepository.toggleBookmarkEvent(event).launchIn(viewModelScope)
    }

    private fun updateFilteredState(events: List<Event>, query: String) {
        val filtered = events.filter { ev ->
            query.isBlank() ||
            ev.title.contains(query, ignoreCase = true) ||
            ev.clubName.contains(query, ignoreCase = true) ||
            ev.location.contains(query, ignoreCase = true)
        }
        _state.value = _state.value.copy(
            pastEvents = events,
            filteredPastEvents = filtered,
            searchQuery = query
        )
    }
}
