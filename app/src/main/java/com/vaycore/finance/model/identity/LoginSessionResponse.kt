package com.vaycore.finance.model.identity

data class LoginSessionResponse(
    val token: String,
    val id: Long,
    val phone: String,
    val appId: Long?,
    val channelId: Long?,
    val passwdSign: Int,
    val activityUrl: String? = null,
)