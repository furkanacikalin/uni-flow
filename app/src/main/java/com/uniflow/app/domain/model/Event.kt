package com.uniflow.app.domain.model

data class Event(
    val id: String = "",
    val universityId: String = "",
    val clubId: String = "",
    val clubName: String = "Yazılım ve Yapay Zeka Kulübü",
    val title: String = "Bahar Hackathonu 2025",
    val description: String = "Yazılım ve yapay zeka alanında fikirlerini projeye dönüştür!",
    val category: String = "Teknoloji & Yazılım",
    val location: String = "Mühendislik Fakültesi Binası, Ana Fuaye",
    val dateText: String = "17–18 Mayıs 2025",
    val dateMonth: String = "MAY",
    val dateDay: String = "17",
    val timeText: String = "36 Saat Kesintisiz",
    val attendeeCount: Int = 0,
    val isFeatured: Boolean = false,
    val isFree: Boolean = true,
    val isBookmarked: Boolean = false,
    val imageUrl: String = ""
) {
    val attendeeCountText: String
        get() = when {
            attendeeCount <= 0 -> "Henüz Katılan Yok"
            attendeeCount == 1 -> "1 Öğrenci Katılıyor"
            else -> "$attendeeCount Öğrenci Katılıyor"
        }

    fun isPast(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return nowMillis > getEndMillis()
    }

    fun getEndMillis(): Long {
        val dt = dateText.trim()
        val monthCodes = mapOf(
            "OCA" to 1, "OCAK" to 1, "JAN" to 1,
            "SUB" to 2, "ŞUBAT" to 2, "SUBAT" to 2, "FEB" to 2,
            "MAR" to 3, "MART" to 3,
            "NIS" to 4, "NİSAN" to 4, "NISAN" to 4, "APR" to 4,
            "MAY" to 5, "MAYIS" to 5,
            "HAZ" to 6, "HAZİRAN" to 6, "HAZIRAN" to 6, "JUN" to 6,
            "TEM" to 7, "TEMMUZ" to 7, "JUL" to 7,
            "AGU" to 8, "AĞUSTOS" to 8, "AGUSTOS" to 8, "AUG" to 8,
            "EYL" to 9, "EYLÜL" to 9, "EYLUL" to 9, "SEP" to 9,
            "EKI" to 10, "EKİM" to 10, "EKIM" to 10, "OCT" to 10,
            "KAS" to 11, "KASIM" to 11, "NOV" to 11,
            "ARA" to 12, "ARALIK" to 12, "DEC" to 12
        )

        var day = 1
        var month = 1
        var year = 2026

        var dateParsed = false
        if (dt.isNotBlank()) {
            val parts = dt.split("\\s+".toRegex())
            if (parts.size >= 3) {
                val dayToken = parts[0]
                val dayStr = if (dayToken.contains("-") || dayToken.contains("–")) {
                    dayToken.split("[-–]".toRegex()).lastOrNull()?.trim() ?: dayToken
                } else dayToken
                val parsedDay = dayStr.toIntOrNull()

                val monthStr = parts[1].uppercase(java.util.Locale.getDefault())
                val parsedMonth = monthCodes[monthStr]

                val parsedYear = parts[2].toIntOrNull()

                if (parsedDay != null && parsedMonth != null && parsedYear != null) {
                    day = parsedDay
                    month = parsedMonth
                    year = parsedYear
                    dateParsed = true
                }
            } else if (dt.contains(".")) {
                val dotParts = dt.split(".")
                if (dotParts.size >= 3) {
                    val pDay = dotParts[0].trim().toIntOrNull()
                    val pMonth = dotParts[1].trim().toIntOrNull()
                    val pYear = dotParts[2].trim().take(4).toIntOrNull()
                    if (pDay != null && pMonth != null && pYear != null) {
                        day = pDay
                        month = pMonth
                        year = pYear
                        dateParsed = true
                    }
                }
            }
        }

        if (!dateParsed) {
            val dayStr = if (dateDay.contains("-") || dateDay.contains("–")) {
                dateDay.split("[-–]".toRegex()).lastOrNull()?.trim() ?: dateDay
            } else dateDay
            day = dayStr.toIntOrNull() ?: 1
            month = monthCodes[dateMonth.uppercase(java.util.Locale.getDefault())] ?: 1
            year = 2026
        }

        var endHour = 23
        var endMinute = 59

        val tt = timeText.trim()
        if (tt.contains("-") || tt.contains("–")) {
            val timeParts = tt.split("[-–]".toRegex())
            if (timeParts.size >= 2) {
                val rightTime = timeParts[1].trim()
                val hourMin = rightTime.split(":")
                if (hourMin.size >= 2) {
                    val h = hourMin[0].trim().toIntOrNull()
                    val m = hourMin[1].trim().take(2).toIntOrNull()
                    if (h != null && m != null) {
                        endHour = h
                        endMinute = m
                    }
                }
            }
        } else if (tt.contains(":")) {
            val hourMin = tt.split(":")
            if (hourMin.size >= 2) {
                val h = hourMin[0].trim().toIntOrNull()
                val m = hourMin[1].trim().take(2).toIntOrNull()
                if (h != null && m != null) {
                    endHour = h
                    endMinute = m
                }
            }
        }

        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, day, endHour, endMinute, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}