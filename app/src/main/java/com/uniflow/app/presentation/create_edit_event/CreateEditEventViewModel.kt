package com.uniflow.app.presentation.create_edit_event

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.repository.AuthRepository
import com.uniflow.app.domain.repository.UniversityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class CreateEditEventState(
    val clubId: String = "",
    val eventId: String = "",
    val isEditMode: Boolean = false,
    val clubName: String = "",
    val universityId: String = "",

    val title: String = "",
    val description: String = "",
    val category: String = "Teknoloji & Yazılım",
    val location: String = "",


    val selectedDateMillis: Long? = null,
    val dateText: String = "",
    val dateMonth: String = "",
    val dateDay: String = "",
    val showDatePicker: Boolean = false,


    val startHour: Int = 14,
    val startMinute: Int = 0,
    val startTimeText: String = "14:00",
    val showStartTimePicker: Boolean = false,

    val endHour: Int = 17,
    val endMinute: Int = 0,
    val endTimeText: String = "17:00",
    val showEndTimePicker: Boolean = false,

    val existingAttendeeCount: Int = 0,
    val isFeatured: Boolean = false,

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val message: String = "",
    val error: String = ""
)

@HiltViewModel
class CreateEditEventViewModel @Inject constructor(
    private val repository: UniversityRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(CreateEditEventState())
    val state: State<CreateEditEventState> = _state

    fun init(clubId: String?, eventId: String?) {
        if (!eventId.isNullOrBlank()) {
            _state.value = _state.value.copy(eventId = eventId, isEditMode = true, isLoading = true)
            loadExistingEvent(eventId)
        } else if (!clubId.isNullOrBlank()) {
            _state.value = _state.value.copy(clubId = clubId, isEditMode = false, isLoading = true)
            loadClubInfo(clubId)
        }
    }

    private fun loadClubInfo(clubId: String) {
        val minCal = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 6)
        }
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(minCal.get(Calendar.YEAR), minCal.get(Calendar.MONTH), minCal.get(Calendar.DAY_OF_MONTH))
        }
        val defaultMillis = utcCal.timeInMillis
        val (formattedDate, dayStr, monthCode) = formatDateMillis(defaultMillis)
        val startH = minCal.get(Calendar.HOUR_OF_DAY)
        val startM = minCal.get(Calendar.MINUTE)
        val endH = (startH + 3) % 24
        val endM = startM

        val startT = String.format(Locale.getDefault(), "%02d:%02d", startH, startM)
        val endT = String.format(Locale.getDefault(), "%02d:%02d", endH, endM)

        repository.getClubDetails(clubId).onEach { result ->
            if (result is Resource.Success && result.data != null) {
                val club = result.data
                _state.value = _state.value.copy(
                    clubId = club.id,
                    clubName = club.name,
                    universityId = club.universityId,
                    selectedDateMillis = defaultMillis,
                    dateText = formattedDate,
                    dateDay = dayStr,
                    dateMonth = monthCode,
                    startHour = startH,
                    startMinute = startM,
                    startTimeText = startT,
                    endHour = endH,
                    endMinute = endM,
                    endTimeText = endT,
                    isLoading = false
                )
            } else if (result is Resource.Error) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.message.toTurkishErrorMessage("Kulüp bilgisi yüklenemedi.")
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun loadExistingEvent(eventId: String) {
        repository.getEventById(eventId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    val ev = result.data
                    if (ev != null) {
                        val times = parseStartAndEndTime(ev.timeText)
                        _state.value = _state.value.copy(
                            clubId = ev.clubId,
                            clubName = ev.clubName,
                            universityId = ev.universityId,
                            title = ev.title,
                            description = ev.description,
                            category = ev.category,
                            location = ev.location,
                            dateText = ev.dateText,
                            dateMonth = ev.dateMonth,
                            dateDay = ev.dateDay,
                            startTimeText = times.first,
                            endTimeText = times.second,
                            existingAttendeeCount = ev.attendeeCount,
                            isFeatured = ev.isFeatured,
                            isLoading = false
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false)
                    }
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message.toTurkishErrorMessage("Etkinlik bilgisi yüklenemedi.")
                    )
                }
                is Resource.Loading -> {}
            }
        }.launchIn(viewModelScope)
    }

    fun onTitleChange(v: String) { _state.value = _state.value.copy(title = v, error = "") }
    fun onDescriptionChange(v: String) { _state.value = _state.value.copy(description = v, error = "") }
    fun onCategoryChange(v: String) { _state.value = _state.value.copy(category = v, error = "") }
    fun onLocationChange(v: String) { _state.value = _state.value.copy(location = v, error = "") }

    // Date Dialog Actions
    fun openDatePicker() { _state.value = _state.value.copy(showDatePicker = true) }
    fun closeDatePicker() { _state.value = _state.value.copy(showDatePicker = false) }

    fun onDateSelected(millis: Long) {
        val (formattedDate, dayStr, monthCode) = formatDateMillis(millis)
        _state.value = _state.value.copy(
            selectedDateMillis = millis,
            dateText = formattedDate,
            dateDay = dayStr,
            dateMonth = monthCode,
            showDatePicker = false,
            error = ""
        )
    }


    fun openStartTimePicker() { _state.value = _state.value.copy(showStartTimePicker = true) }
    fun closeStartTimePicker() { _state.value = _state.value.copy(showStartTimePicker = false) }

    fun onStartTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        _state.value = _state.value.copy(
            startHour = hour,
            startMinute = minute,
            startTimeText = formattedTime,
            showStartTimePicker = false,
            error = ""
        )
    }

    fun openEndTimePicker() { _state.value = _state.value.copy(showEndTimePicker = true) }
    fun closeEndTimePicker() { _state.value = _state.value.copy(showEndTimePicker = false) }

    fun onEndTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        _state.value = _state.value.copy(
            endHour = hour,
            endMinute = minute,
            endTimeText = formattedTime,
            showEndTimePicker = false,
            error = ""
        )
    }

    fun saveEvent() {
        val s = _state.value
        val title = s.title.trim()
        val dateText = s.dateText.trim()
        val location = s.location.trim()

        if (title.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen etkinlik başlığını giriniz.")
            return
        }
        if (dateText.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen geçerli bir etkinlik tarihi seçiniz.")
            return
        }


        if (!s.isEditMode && s.selectedDateMillis != null) {
            val selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = s.selectedDateMillis
            }
            val eventStartCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, s.startHour)
                set(Calendar.MINUTE, s.startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val minAllowedMillis = System.currentTimeMillis() + (6 * 60 * 60 * 1000L)
            if (eventStartCal.timeInMillis < minAllowedMillis) {
                val minCal = Calendar.getInstance().apply { timeInMillis = minAllowedMillis }
                val minDateStr = String.format(
                    Locale.getDefault(),
                    "%02d.%02d.%d %02d:%02d",
                    minCal.get(Calendar.DAY_OF_MONTH),
                    minCal.get(Calendar.MONTH) + 1,
                    minCal.get(Calendar.YEAR),
                    minCal.get(Calendar.HOUR_OF_DAY),
                    minCal.get(Calendar.MINUTE)
                )
                _state.value = _state.value.copy(
                    error = "Etkinlik başlangıcı şu andan en az 6 saat sonrası olmalıdır. (En erken: $minDateStr)"
                )
                return
            }

            val eventEndCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, s.endHour)
                set(Calendar.MINUTE, s.endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (eventEndCal.timeInMillis <= eventStartCal.timeInMillis) {
                _state.value = _state.value.copy(
                    error = "Etkinlik bitiş saati, başlangıç saatinden sonra olmalıdır."
                )
                return
            }
        }

        val combinedTime = "${s.startTimeText} - ${s.endTimeText}"

        val targetEvent = Event(
            id = if (s.isEditMode) s.eventId else "",
            universityId = s.universityId,
            clubId = s.clubId,
            clubName = s.clubName.ifBlank { "Kulüp Etkinliği" },
            title = title,
            description = s.description.trim(),
            category = s.category,
            location = location.ifBlank { "Kampüs Yerleşkesi" },
            dateText = dateText,
            dateMonth = s.dateMonth.ifBlank { "MAY" },
            dateDay = s.dateDay.ifBlank { "17" },
            timeText = combinedTime,
            attendeeCount = s.existingAttendeeCount,
            isFeatured = s.isFeatured,
            isFree = true
        )

        val flow = if (s.isEditMode) repository.updateEvent(targetEvent) else repository.createEvent(targetEvent)
        flow.onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = _state.value.copy(isSaving = true, error = "")
                }
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        isSavedSuccess = true,
                        message = if (s.isEditMode) "Etkinlik başarıyla güncellendi!" else "Yeni etkinlik başarıyla yayınlandı!"
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

    private fun formatDateMillis(millis: Long): Triple<String, String, String> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val monthNames = arrayOf("", "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
        val monthCodes = arrayOf("", "OCA", "SUB", "MAR", "NIS", "MAY", "HAZ", "TEM", "AGU", "EYL", "EKI", "KAS", "ARA")

        val mName = if (month in 1..12) monthNames[month] else "Mayıs"
        val mCode = if (month in 1..12) monthCodes[month] else "MAY"

        val formattedDate = "$day $mName $year"
        return Triple(formattedDate, day.toString(), mCode)
    }

    private fun parseStartAndEndTime(timeStr: String): Pair<String, String> {
        if (timeStr.contains("-")) {
            val parts = timeStr.split("-")
            if (parts.size >= 2) {
                return Pair(parts[0].trim(), parts[1].trim())
            }
        }
        return Pair("14:00", "17:00")
    }
}
