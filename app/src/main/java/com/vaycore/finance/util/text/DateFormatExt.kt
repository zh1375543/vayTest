package com.vaycore.finance.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.toYmdDateString(): String {
    val parts = this.split("-")
    return if (parts.size == 3) {
        val day = parts[0].padStart(2, '0')
        val month = parts[1].padStart(2, '0')
        val year = parts[2]
        "$year-$month-$day"
    } else {
        this
    }
}

fun String.toDmyDateString(): String {
    val parts = this.split("-")
    return if (parts.size == 3) {
        val day = parts[2].padStart(2, '0')
        val month = parts[1].padStart(2, '0')
        val year = parts[0]
        "$year-$month-$day"
    } else {
        this
    }
}

fun Long.formatDateString(pattern: String = "dd/MM/yyyy"): String {
    return try {
        val date = Date(this)
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.format(date)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}
