package com.vaycore.finance.util

import java.util.Calendar

private val PHONE_NUMBER_REGEX = Regex("^0?9\\d{9}$")
private val ID_CARD_REGEX = Regex("^\\d{12}$")

fun String.isPhoneNumberValid(): Boolean {
    return PHONE_NUMBER_REGEX.matches(this)
}

fun String.isIdCardValid(): Boolean {
//    val pattern = Regex("^(\\d{9}|\\d{12})$")
    return ID_CARD_REGEX.matches(this)
}

fun String.isAdult(): Boolean {
    val parts = split("-")
    if (parts.size != 3) return false

    val day = parts[0].toIntOrNull() ?: return false
    val month = parts[1].toIntOrNull() ?: return false
    val year = parts[2].toIntOrNull() ?: return false
    if (year <= 0 || month !in 1..12 || day <= 0) return false

    val birth = Calendar.getInstance().apply {
        isLenient = false
        clear()
        set(year, month - 1, day) // Calendar months are 0-based
    }

    return try {
        birth.timeInMillis // Force strict validation for dates such as 31-02-2000.

        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - year
        if (today.get(Calendar.MONTH) < month - 1 ||
            (today.get(Calendar.MONTH) == month - 1 &&
                    today.get(Calendar.DAY_OF_MONTH) < day)
        ) {
            age--
        }
        age >= 18
    } catch (_: IllegalArgumentException) {
        false
    }
}

fun String.isEmailValid(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}
