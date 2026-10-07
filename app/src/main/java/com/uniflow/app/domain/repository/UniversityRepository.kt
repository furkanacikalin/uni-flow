package com.uniflow.app.domain.repository

import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.model.University
import com.uniflow.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UniversityRepository {
    fun getUniversities(): Flow<Resource<List<University>>>
    fun getClubsByUniversity(universityId: String): Flow<Resource<List<Club>>>
    fun getEventsByUniversity(universityId: String): Flow<Resource<List<Event>>>
    fun getClubDetails(clubId: String): Flow<Resource<Club>>
    fun updateClubDetails(
        clubId: String,
        name: String,
        category: String,
        description: String,
        socialLinks: Map<String, String>
    ): Flow<Resource<Unit>>
    fun getEventsByClub(clubId: String): Flow<Resource<List<Event>>>
    fun getEventById(eventId: String): Flow<Resource<Event>>
    fun getEventAttendees(eventId: String): Flow<Resource<List<UserProfile>>>
    fun joinEvent(event: Event): Flow<Resource<Unit>>
    fun isUserJoinedEvent(eventId: String): Flow<Boolean>
    fun createEvent(event: Event): Flow<Resource<Unit>>
    fun updateEvent(event: Event): Flow<Resource<Unit>>
    fun deleteEvent(eventId: String): Flow<Resource<Unit>>
    fun joinClub(club: Club): Flow<Resource<Unit>>
    fun isUserJoinedClub(clubId: String): Flow<Boolean>
    fun getClubMembers(clubId: String): Flow<Resource<List<UserProfile>>>
}