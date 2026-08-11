package com.vaycore.finance.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun String.toYmdDateString(): String {
    val date = parseDmyDateParts() ?: return this
    return String.format(
        Locale.ENGLISH,
        "%04d-%02d-%02d",
        date.year,
        date.month,
        date.day,
    )
}

fun String.toDmyDateString(): String {
    val date = parseYmdDateParts() ?: parseDmyDateParts() ?: return this
    return "${date.day}-${MONTH_ABBREVIATIONS[date.month - 1]}-${date.year}"
}

internal data class DateParts(
    val day: Int,
    val month: Int,
    val year: Int,
)

internal fun String.parseDmyDateParts(): DateParts? {
    val parts = trim().split("-")
    if (parts.size != 3) return null

    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull()
        ?: (MONTH_ABBREVIATIONS.indexOf(parts[1].uppercase(Locale.ENGLISH)) + 1)
            .takeIf { it > 0 }
        ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return validatedDateParts(day, month, year)
}

private fun String.parseYmdDateParts(): DateParts? {
    val parts = trim().split("-")
    if (parts.size != 3 || parts[0].length != 4) return null

    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return validatedDateParts(day, month, year)
}

private fun validatedDateParts(day: Int, month: Int, year: Int): DateParts? {
    if (year <= 0 || month !in 1..12 || day <= 0) return null
    return try {
        Calendar.getInstance().apply {
            isLenient = false
            clear()
            set(year, month - 1, day)
            timeInMillis
        }
        DateParts(day, month, year)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private val MONTH_ABBREVIATIONS = listOf(
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
)

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
