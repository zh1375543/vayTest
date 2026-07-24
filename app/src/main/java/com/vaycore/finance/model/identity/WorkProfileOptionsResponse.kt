package com.vaycore.finance.model.identity

import com.vaycore.finance.data.bean.SelectionOption

data class WorkProfileOptionsResponse(
    val relatives: MutableList<SelectionOption>? = null,
    val salaryRange: MutableList<SelectionOption>? = null,
    val otherRelatives: MutableList<SelectionOption>? = null,
    val jobnature: MutableList<SelectionOption>? = null,
    val staffSize: MutableList<SelectionOption>? = null,
    val industry: MutableList<SelectionOption>? = null,
)