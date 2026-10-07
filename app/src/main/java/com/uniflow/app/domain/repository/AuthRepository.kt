package com.uniflow.app.domain.repository

import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.University
import kotlinx.coroutines.flow.Flow

import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.model.ClubApplication
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.model.EventTicket
import com.uniflow.app.domain.model.UserProfile

interface AuthRepository {
    fun login(email: String, password: String, rememberMe: Boolean): Flow<Resource<Unit>>
    fun register(email: String, password: String): Flow<Resource<Unit>>
    fun isUserLoggedIn(): Boolean
    fun logout()
    fun getSelectedUniversity(): Flow<Resource<University?>>
    fun saveSelectedUniversity(university: University): Flow<Resource<Unit>>
    fun getUserProfile(): Flow<Resource<UserProfile?>>
    fun saveUserProfile(
        firstName: String,
        lastName: String,
        studentNo: String,
        faculty: String,
        department: String,
        grade: String
    ): Flow<Resource<Unit>>
    fun updateEditableProfile(
        firstName: String,
        lastName: String,
        profileImageUrl: String?
    ): Flow<Resource<Unit>>
    fun updateUserEmail(newEmail: String): Flow<Resource<Unit>>
    fun updateUserPassword(newPassword: String): Flow<Resource<Unit>>
    fun sendPasswordResetEmail(email: String): Flow<Resource<Unit>>
    fun submitClubApplication(
        clubName: String,
        clubCategory: String,
        clubDescription: String,
        documentFileName: String,
        documentUri: String
    ): Flow<Resource<Unit>>
    fun getClubApplications(): Flow<Resource<List<ClubApplication>>>
    fun getUserClubApplications(): Flow<Resource<List<ClubApplication>>>
    fun updateApplicationStatus(
        applicationId: String,
        status: String,
        application: ClubApplication
    ): Flow<Resource<Unit>>

    fun joinClub(club: Club): Flow<Resource<Unit>>
    fun leaveClub(clubId: String): Flow<Resource<Unit>>
    fun getUserJoinedClubs(): Flow<Resource<List<Club>>>
    fun getUserManagedClubs(): Flow<Resource<List<Club>>>
    fun getUserTickets(): Flow<Resource<List<EventTicket>>>
    fun getUserBookmarkedEvents(): Flow<Resource<List<Event>>>
    fun toggleBookmarkEvent(event: Event): Flow<Resource<Unit>>
    fun getUserPastEvents(): Flow<Resource<List<EventTicket>>>
}
