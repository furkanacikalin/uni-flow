package com.uniflow.app.domain.model

data class EventTicket(
    val id: String = "",
    val eventTitle: String = "",
    val category: String = "",
    val dateMonth: String = "MAY",
    val dateDay: String = "17",
    val time: String = "10:00",
    val location: String = "",
    val deskNo: String = "",
    val isQrReady: Boolean = true
)
