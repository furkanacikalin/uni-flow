package com.uniflow.app.presentation.complete_profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniflow.app.core.common.Resource
import com.uniflow.app.core.common.toTurkishErrorMessage
import com.uniflow.app.domain.model.University
import com.uniflow.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class CompleteProfileState(
    val currentStep: Int = 1,
    val faculty: String = "",
    val department: String = "",
    val grade: String = "1. Sınıf",
    val firstName: String = "",
    val lastName: String = "",
    val studentNo: String = "",
    val university: University? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = mutableStateOf(CompleteProfileState())
    val state: State<CompleteProfileState> = _state

    init {
        loadSelectedUniversity()
    }

    private fun loadSelectedUniversity() {
        authRepository.getSelectedUniversity().onEach { result ->
            if (result is Resource.Success && result.data != null) {
                _state.value = _state.value.copy(university = result.data)
            }
        }.launchIn(viewModelScope)
    }

    fun onFacultyChange(value: String) {
        _state.value = _state.value.copy(faculty = value, error = "")
    }

    fun onDepartmentChange(value: String) {
        _state.value = _state.value.copy(department = value, error = "")
    }

    fun onGradeChange(value: String) {
        _state.value = _state.value.copy(grade = value, error = "")
    }

    fun onFirstNameChange(value: String) {
        _state.value = _state.value.copy(firstName = value, error = "")
    }

    fun onLastNameChange(value: String) {
        _state.value = _state.value.copy(lastName = value, error = "")
    }

    fun onStudentNoChange(value: String) {
        _state.value = _state.value.copy(studentNo = value, error = "")
    }

    fun goToNextStep() {
        val fac = _state.value.faculty.trim()
        val dept = _state.value.department.trim()
        val grd = _state.value.grade.trim()

        if (fac.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen fakültenizi giriniz.")
            return
        }
        if (dept.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen bölümünüzü giriniz.")
            return
        }
        if (grd.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen sınıfınızı seçiniz.")
            return
        }

        _state.value = _state.value.copy(currentStep = 2, error = "")
    }

    fun goToPreviousStep() {
        _state.value = _state.value.copy(currentStep = 1, error = "")
    }

    fun saveProfile() {
        val fName = _state.value.firstName.trim()
        val lName = _state.value.lastName.trim()
        val sNo = _state.value.studentNo.trim()

        if (fName.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen adınızı giriniz.")
            return
        }
        if (lName.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen soyadınızı giriniz.")
            return
        }
        if (sNo.isBlank()) {
            _state.value = _state.value.copy(error = "Lütfen öğrenci numaranızı giriniz.")
            return
        }

        authRepository.saveUserProfile(
            firstName = fName,
            lastName = lName,
            studentNo = sNo,
            faculty = _state.value.faculty.trim(),
            department = _state.value.department.trim(),
            grade = _state.value.grade.trim()
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
                        error = result.message.toTurkishErrorMessage("Profil kaydedilemedi.")
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
