package com.vaycore.finance.model.identity

data class AuthOptionResponse(
    var src: Int = 0,
    var title: String = "",
    var type: String = "",
    var isCertified: Boolean = false,
    val authConfig: String? = "",
)