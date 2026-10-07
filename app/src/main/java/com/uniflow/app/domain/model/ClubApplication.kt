package com.uniflow.app.domain.model

data class ClubApplication(
    val id: String = "",
    val applicantUid: String = "",
    val applicantName: String = "",
    val applicantEmail: String = "",
    val studentNo: String = "",
    val universityId: String = "",
    val universityName: String = "",
    val clubName: String = "",
    val clubCategory: String = "AKADEMİK",
    val clubDescription: String = "",
    val documentFileName: String = "",
    val documentUri: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis()
)
