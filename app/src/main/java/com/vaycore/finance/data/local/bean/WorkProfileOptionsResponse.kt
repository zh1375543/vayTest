package com.vaycore.finance.data.local.bean

data class WorkProfileOptionsResponse(
    val relatives: MutableList<SelectionOption>? = null,
    val salaryRange: MutableList<SelectionOption>? = null,
    val otherRelatives: MutableList<SelectionOption>? = null,
    val jobnature: MutableList<SelectionOption>? = null,
    val staffSize: MutableList<SelectionOption>? = null,
    val industry: MutableList<SelectionOption>? = null,
)
