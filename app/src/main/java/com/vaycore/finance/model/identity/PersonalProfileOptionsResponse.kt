package com.vaycore.finance.model.identity

import com.vaycore.finance.data.bean.SelectionOption

data class PersonalProfileOptionsResponse(
    val gender: MutableList<SelectionOption>? = null,
    val language: MutableList<SelectionOption>? = null,
    val maritalStatus: MutableList<SelectionOption>? = null,
    val purpose: MutableList<SelectionOption>? = null,
    val education: MutableList<SelectionOption>? = null,
    val workTime: MutableList<SelectionOption>? = null,
)
