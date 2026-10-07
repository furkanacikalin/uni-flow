package com.uniflow.app.domain.model

data class Club(
    val id: String = "",
    val universityId: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val instagramUrl: String = "",
    val socialLinks: Map<String, String> = emptyMap(),
    val memberCount: Int = 1,
    val eventCount: Int = 0,
    val isJoined: Boolean = false,
    val leaderUid: String = ""
)