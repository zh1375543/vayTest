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
    val date = parseDmyDateParts() ?: return false
    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - date.year
    if (today.get(Calendar.MONTH) < date.month - 1 ||
        (today.get(Calendar.MONTH) == date.month - 1 &&
            today.get(Calendar.DAY_OF_MONTH) < date.day)
    ) {
        age--
    }
    return age >= 18
}

fun String.isEmailValid(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}
