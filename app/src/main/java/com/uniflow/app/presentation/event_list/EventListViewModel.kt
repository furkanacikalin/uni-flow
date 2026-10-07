package com.uniflow.app.presentation.event_list

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

import java.util.Calendar
import java.util.Locale

enum class EventSortOption(val label: String) {
    DATE_ASC("Artan Tarih (Varsayılan)"),
    DATE_DESC("Azalan Tarih"),
    ALPHA_ASC("Alfabetik (A-Z)"),
    ALPHA_DESC("Alfabetik (Z-A)"),
    ATTENDEES_ASC("Artan Katılımcı"),
    ATTENDEES_DESC("Azalan Katılımcı")
}

data class EventListState(
    val events: List<Event> = emptyList(),
    val filteredEvents: List<Event> = emptyList(),
    val todayEvents: List<Event> = emptyList(),
    val otherEvents: List<Event> = emptyList(),
    val userJoinedEventIds: Set<String> = emptySet(),
    val userBookmarkedEventIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedCategory: String = "Tümü",
    val selectedSortOption: EventSortOption = EventSortOption.DATE_ASC,
    val isLoading: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val getEventsByUniversityUseCase: GetEventsByUniversityUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(EventListState())
    val state: State<EventListState> = _state

    fun loadEvents(universityId: String) {
        authRepository.getUserTickets().onEach { ticketRes ->
            if (ticketRes is Resource.Success) {
                val ticketIds = (ticketRes.data ?: emptyList()).map { it.id }.filter { it.isNotBlank() }.toSet()
                val updatedEvents = _state.value.events.map { ev ->
                    if (ev.id.isNotBlank() && ticketIds.contains(ev.id) && ev.attendeeCount == 0) ev.copy(attendeeCount = 1) else ev
                }
                _state.value = _state.value.copy(userJoinedEventIds = ticketIds)
                updateFilteredState(updatedEvents, _state.value.searchQuery, _state.value.selectedCategory)
            }
        }.launchIn(viewModelScope)

        authRepository.getUserBookmarkedEvents().onEach { bookmarkRes ->
            if (bookmarkRes is Resource.Success) {
                val bookmarkedIds = (bookmarkRes.data ?: emptyList()).map { it.id }.filter { it.isNotBlank() }.toSet()
                val updatedEvents = _state.value.events.map { ev ->
                    ev.copy(isBookmarked = bookmarkedIds.contains(ev.id))
                }
                _state.value = _state.value.copy(userBookmarkedEventIds = bookmarkedIds)
                updateFilteredState(updatedEvents, _state.value.searchQuery, _state.value.selectedCategory)
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

                    val allEvents = rawEvents.map { ev ->
                        var updated = if (ev.id.isNotBlank() && joinedIds.contains(ev.id) && ev.attendeeCount == 0) ev.copy(attendeeCount = 1) else ev
                        if (ev.id.isNotBlank() && bookmarkedIds.contains(ev.id)) {
                            updated = updated.copy(isBookmarked = true)
                        }
                        updated
                    }

                    _state.value = _state.value.copy(isLoading = false)
                    updateFilteredState(allEvents, _state.value.searchQuery, _state.value.selectedCategory)
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        events = emptyList(),
                        todayEvents = emptyList(),
                        otherEvents = emptyList(),
                        filteredEvents = emptyList(),
                        isLoading = false
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        updateFilteredState(_state.value.events, query, _state.value.selectedCategory)
    }

    fun onCategoryChange(category: String) {
        updateFilteredState(_state.value.events, _state.value.searchQuery, category)
    }

    fun onSortOptionChange(sortOption: EventSortOption) {
        updateFilteredState(_state.value.events, _state.value.searchQuery, _state.value.selectedCategory, sortOption)
    }

    fun toggleBookmark(event: Event) {
        val newBookmarkedState = !event.isBookmarked
        val updatedEvents = _state.value.events.map {
            if (it.id == event.id) it.copy(isBookmarked = newBookmarkedState) else it
        }
        val updatedBookmarkedIds = if (newBookmarkedState) {
            _state.value.userBookmarkedEventIds + event.id
        } else {
            _state.value.userBookmarkedEventIds - event.id
        }

        _state.value = _state.value.copy(userBookmarkedEventIds = updatedBookmarkedIds)
        updateFilteredState(updatedEvents, _state.value.searchQuery, _state.value.selectedCategory)

        authRepository.toggleBookmarkEvent(event).launchIn(viewModelScope)
    }

    private fun updateFilteredState(
        allEvents: List<Event>,
        query: String,
        category: String,
        sortOption: EventSortOption = _state.value.selectedSortOption
    ) {
        val activeEvents = allEvents.filter { !it.isPast() }
        val matchingEvents = filterEvents(activeEvents, query, category)
        val sortedMatchingEvents = sortEvents(matchingEvents, sortOption)

        val todayEvents = sortedMatchingEvents.filter { isTodayEvent(it) }
        val todayEventIds = todayEvents.map { it.id }.toSet()
        val otherEvents = sortedMatchingEvents.filter { !todayEventIds.contains(it.id) }

        _state.value = _state.value.copy(
            events = allEvents,
            todayEvents = todayEvents,
            otherEvents = otherEvents,
            filteredEvents = sortedMatchingEvents,
            searchQuery = query,
            selectedCategory = category,
            selectedSortOption = sortOption
        )
    }

    private fun sortEvents(list: List<Event>, sortOption: EventSortOption): List<Event> {
        return when (sortOption) {
            EventSortOption.DATE_ASC -> list.sortedBy { parseEventDateMillis(it) }
            EventSortOption.DATE_DESC -> list.sortedByDescending { parseEventDateMillis(it) }
            EventSortOption.ALPHA_ASC -> list.sortedBy { it.title.lowercase(Locale.getDefault()) }
            EventSortOption.ALPHA_DESC -> list.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            EventSortOption.ATTENDEES_ASC -> list.sortedBy { it.attendeeCount }
            EventSortOption.ATTENDEES_DESC -> list.sortedByDescending { it.attendeeCount }
        }
    }

    private fun parseEventDateMillis(event: Event): Long {
        val dt = event.dateText.trim()
        val monthCodes = mapOf(
            "OCA" to 1, "OCAK" to 1,
            "SUB" to 2, "ŞUBAT" to 2, "SUBAT" to 2,
            "MAR" to 3, "MART" to 3,
            "NIS" to 4, "NİSAN" to 4, "NISAN" to 4,
            "MAY" to 5, "MAYIS" to 5,
            "HAZ" to 6, "HAZİRAN" to 6, "HAZIRAN" to 6,
            "TEM" to 7, "TEMMUZ" to 7,
            "AGU" to 8, "AĞUSTOS" to 8, "AGUSTOS" to 8,
            "EYL" to 9, "EYLÜL" to 9, "EYLUL" to 9,
            "EKI" to 10, "EKİM" to 10, "EKIM" to 10,
            "KAS" to 11, "KASIM" to 11,
            "ARA" to 12, "ARALIK" to 12
        )

        try {
            val parts = dt.split(" ")
            if (parts.size >= 3) {
                val day = parts[0].toIntOrNull() ?: 1
                val monthStr = parts[1].uppercase(Locale.getDefault())
                val month = monthCodes[monthStr] ?: 1
                val year = parts[2].toIntOrNull() ?: 2026

                val cal = Calendar.getInstance()
                cal.set(year, month - 1, day, 0, 0, 0)
                return cal.timeInMillis
            }
        } catch (e: Exception) {

        }

        try {
            val day = event.dateDay.toIntOrNull() ?: 1
            val monthCode = event.dateMonth.uppercase(Locale.getDefault())
            val month = monthCodes[monthCode] ?: 1
            val cal = Calendar.getInstance()
            cal.set(2026, month - 1, day, 0, 0, 0)
            return cal.timeInMillis
        } catch (e: Exception) {
            return 0L
        }
    }

    private fun isTodayEvent(event: Event): Boolean {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val monthNames = arrayOf("", "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
        val monthCodes = arrayOf("", "OCA", "SUB", "MAR", "NIS", "MAY", "HAZ", "TEM", "AGU", "EYL", "EKI", "KAS", "ARA")

        val mName = if (month in 1..12) monthNames[month] else ""
        val mCode = if (month in 1..12) monthCodes[month] else ""

        val dt = event.dateText.lowercase()
        val dayStr = day.toString()
        val dayFormattedStr = String.format(Locale.getDefault(), "%02d", day)

        val matchesText = dt.contains("$day $mName".lowercase()) ||
                dt.contains("$dayFormattedStr $mName".lowercase()) ||
                dt.contains("$day.$month.$year") ||
                dt.contains("$dayFormattedStr.$month.$year") ||
                dt.contains("$dayFormattedStr.${String.format(Locale.getDefault(), "%02d", month)}.$year")

        val matchesDayMonth = (event.dateDay == dayStr || event.dateDay == dayFormattedStr) &&
                (event.dateMonth.equals(mCode, ignoreCase = true) || event.dateMonth.equals(mName, ignoreCase = true))

        return matchesText || matchesDayMonth
    }

    private fun filterEvents(list: List<Event>, query: String, category: String): List<Event> {
        return list.filter { matchesSingleEvent(it, query, category) }
    }

    private fun matchesSingleEvent(event: Event, query: String, category: String): Boolean {
        val matchesQuery = query.isBlank() ||
            event.title.contains(query, ignoreCase = true) ||
            event.clubName.contains(query, ignoreCase = true) ||
            event.location.contains(query, ignoreCase = true)

        val matchesCategory = when (category) {
            "Tümü" -> true
            "Bugün" -> {
                val todayCal = Calendar.getInstance()
                val day = todayCal.get(Calendar.DAY_OF_MONTH).toString()
                event.dateDay == day || event.dateText.contains(day)
            }
            else -> {
                val catLower = category.lowercase()
                val evCatLower = event.category.lowercase()
                evCatLower == catLower ||
                evCatLower.contains(catLower) ||
                catLower.contains(evCatLower) ||
                (catLower.contains("yazılım") && (evCatLower.contains("yazılım") || evCatLower.contains("teknoloji"))) ||
                (catLower.contains("kültür") && (evCatLower.contains("kültür") || evCatLower.contains("sanat") || evCatLower.contains("müzik"))) ||
                (catLower.contains("spor") && (evCatLower.contains("spor") || evCatLower.contains("sağlık"))) ||
                (catLower.contains("kariyer") && (evCatLower.contains("kariyer") || evCatLower.contains("akademik"))) ||
                (catLower.contains("sosyal") && (evCatLower.contains("sosyal") || evCatLower.contains("eğlence")))
            }
        }

        return matchesQuery && matchesCategory
    }
}
