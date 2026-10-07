package com.uniflow.app.domain.model

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val faculty: String = "",
    val department: String = "",
    val grade: String = "1. Sınıf",
    val studentNo: String = "",
    val campus: String = "Ana Kampüs",
    val profileImageUrl: String = "",
    val joinedClubsCount: Int = 0,
    val attendedEventsCount: Int = 0,
    val campusPoints: Int = 0,
    val isVerified: Boolean = true,
    val isProfileComplete: Boolean = false,
    val isAdmin: Boolean = false
) {
    val fullName: String
        get() = if (firstName.isNotBlank() || lastName.isNotBlank()) "$firstName $lastName".trim() else "Öğrenci"
}
