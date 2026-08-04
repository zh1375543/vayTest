package com.vaycore.finance.model.identity

import com.vaycore.finance.data.bean.SelectionOption

data class PersonalProfileOptionsResponse(
    val gender: MutableList<SelectionOption>? = null,
    val language: MutableList<SelectionOption>? = null,
    val maritalStatus: MutableList<SelectionOption>? = null,
    val purpose: MutableList<SelectionOption>? = null,
    val education: MutableList<SelectionOption>? = null,
    val idCardTypeV2: MutableList<IdCardTypeOption>? = null,
    val workTime: MutableList<SelectionOption>? = null,
)

data class IdCardTypeOption(
    val state: String? = null,
    val info: String? = null,
)
