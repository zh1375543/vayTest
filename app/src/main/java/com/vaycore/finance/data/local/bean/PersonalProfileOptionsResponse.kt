package com.vaycore.finance.data.local.bean

data class PersonalProfileOptionsResponse(
    val gender: MutableList<SelectionOption>? = null,
    val language: MutableList<SelectionOption>? = null,
    val maritalStatus: MutableList<SelectionOption>? = null,
    val purpose: MutableList<SelectionOption>? = null,
    val education: MutableList<SelectionOption>? = null,
)
