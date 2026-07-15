package com.vaycore.finance.data.local.bean

data class LoginSessionResponse(
    val token: String,
    val id: Long,
    val phone: String,
    val appId: Long?,
    val channelId: Long?,
    val passwdSign: Int,
)
